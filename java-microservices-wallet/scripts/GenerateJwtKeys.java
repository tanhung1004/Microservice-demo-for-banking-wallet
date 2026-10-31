import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GenerateJwtKeys {

    public static void main(String[] args) throws Exception {
        Path directory = Path.of("secrets");
        Path privateKeyFile = directory.resolve("jwt-private.pem");
        Path publicKeyFile = directory.resolve("jwt-public.pem");

        // Không ghi đè khóa cũ khi chạy lại.
        if (Files.exists(privateKeyFile) || Files.exists(publicKeyFile)) {
            throw new IllegalStateException(
                    "Key files already exist. Existing keys were not changed."
            );
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        Files.createDirectories(directory);

        writePem(
                privateKeyFile,
                "PRIVATE KEY",
                keyPair.getPrivate().getEncoded()
        );

        writePem(
                publicKeyFile,
                "PUBLIC KEY",
                keyPair.getPublic().getEncoded()
        );

        System.out.println("Created secrets/jwt-private.pem");
        System.out.println("Created secrets/jwt-public.pem");
    }

    private static void writePem(
            Path file,
            String label,
            byte[] encodedKey
    ) throws Exception {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(encodedKey);

        String pem = "-----BEGIN " + label + "-----\n"
                + body
                + "\n-----END " + label + "-----\n";

        Files.writeString(
                file,
                pem,
                StandardCharsets.US_ASCII,
                StandardOpenOption.CREATE_NEW
        );
    }
}