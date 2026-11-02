package com.tommy.wallet.wallet;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import com.tommy.wallet.event.MoneyDepositedEvent;
import com.tommy.wallet.messaging.DepositEventPublisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DepositTransactionTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-4111-8111-111111111111");

    private AnnotationConfigApplicationContext context;
    private WalletService walletService;
    private TransactionTemplate transaction;
    private KafkaTemplate<String, String> kafka;

    // Ghi nhận event đã được service phát trong transaction.
    private AtomicReference<MoneyDepositedEvent> observedEvent;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);

        walletService = context.getBean(WalletService.class);
        kafka = context.getBean(KafkaTemplate.class);

        transaction = new TransactionTemplate(
                context.getBean(PlatformTransactionManager.class)
        );

        observedEvent = new AtomicReference<>();

        context.addApplicationListener(event -> {
            if (event instanceof PayloadApplicationEvent<?> payload
                    && payload.getPayload() instanceof MoneyDepositedEvent deposit) {
                observedEvent.set(deposit);
            }
        });

        JdbcTemplate jdbc = new JdbcTemplate(
                context.getBean(DataSource.class)
        );

        // Chỉ tạo bảng trong H2 của lượt test này.
        jdbc.execute("""
                CREATE TABLE wallets (
                    user_id UUID PRIMARY KEY,
                    balance BIGINT NOT NULL,
                    currency VARCHAR(3) NOT NULL
                )
                """);

        jdbc.update(
                "INSERT INTO wallets (user_id, balance, currency) VALUES (?, ?, ?)",
                USER_ID,
                100000L,
                "VND"
        );

        // Không kết nối Kafka thật.
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
                        // Xóa lịch sử gọi lúc Spring khởi tạo bean trước khi test nghiệp vụ.
        // Giữ nguyên cấu hình mock ở trên.
        clearInvocations(kafka);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void commitSendsEventOnlyAfterTransactionCompletes() {
        transaction.executeWithoutResult(status -> {
            walletService.deposit(USER_ID, 1000L);

            assertEquals(101000L, balance());
            assertNotNull(observedEvent.get());

            // DB chưa commit: chưa được gửi Kafka.
            verifyNoInteractions(kafka);
        });

        // Transaction đã commit: giữ số dư mới và gửi đúng event.
        assertEquals(101000L, balance());

        var jsonCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);

        verify(kafka, times(1)).send(
                eq("wallet-deposited"),
                eq(USER_ID.toString()),
                jsonCaptor.capture()
        );

        var json = JsonMapper.builder().build()
                .readTree(jsonCaptor.getValue());

        assertEquals(
                observedEvent.get().transactionId().toString(),
                json.get("transactionId").asText()
        );
        assertEquals(1000L, json.get("amount").asLong());
        assertEquals(101000L, json.get("balanceAfter").asLong());

        verifyNoMoreInteractions(kafka);
    }

    @Test
    void rollbackRestoresBalanceAndDoesNotSendEvent() {
        RuntimeException forcedFailure =
                new RuntimeException("Intentional failure after deposit");

        RuntimeException actual = assertThrows(
                RuntimeException.class,
                () -> transaction.executeWithoutResult(status -> {
                    walletService.deposit(USER_ID, 1000L);

                    // Chứng minh đã cập nhật DB và phát event trong transaction.
                    assertEquals(101000L, balance());
                    assertNotNull(observedEvent.get());
                    verifyNoInteractions(kafka);

                    // Lỗi xảy ra SAU khi service đăng ký event.
                    throw forcedFailure;
                })
        );

        assertSame(forcedFailure, actual);

        // Sau rollback, DB trở lại số dư ban đầu.
        assertEquals(100000L, balance());

        // AFTER_COMMIT không được gửi sự kiện của giao dịch rollback.
        verifyNoInteractions(kafka);
    }

    @Test
    void rejectedDepositDoesNotChangeBalanceOrPublishEvent() {
        assertThrows(
                WalletOperationException.class,
                () -> walletService.deposit(USER_ID, 0L)
        );

        assertEquals(100000L, balance());
        assertNull(observedEvent.get());
        verifyNoInteractions(kafka);
    }

    private long balance() {
        return walletService.getBalance(USER_ID).balance();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfig {

        @Bean(destroyMethod = "shutdown")
        EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        JdbcClient jdbcClient(DataSource dataSource) {
            return JdbcClient.create(dataSource);
        }

        @Bean
        WalletRepository walletRepository(JdbcClient jdbcClient) {
            return new WalletRepository(jdbcClient);
        }

        @Bean
        WalletService walletService(
                WalletRepository repository,
                ApplicationEventPublisher publisher
        ) {
            return new WalletService(repository, publisher);
        }

        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }

        @Bean
        DepositEventPublisher depositEventPublisher(
                KafkaTemplate<String, String> kafkaTemplate
        ) {
            return new DepositEventPublisher(
                    kafkaTemplate,
                    JsonMapper.builder().build(),
                    "wallet-deposited"
            );
        }
    }
}