import { useCallback, useEffect, useRef, useState } from 'react'
import { apiRequest } from './service/api.js'
import './App.css'

function formatMoney(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value)
}

// ==================== ĐĂNG NHẬP ====================

function LoginForm({ onLogin, notice }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const submitting = useRef(false)

  async function handleSubmit(event) {
    event.preventDefault()

    if (submitting.current) return

    if (!username.trim() || !password) {
      setError('Vui lòng nhập tên đăng nhập và mật khẩu.')
      return
    }

    submitting.current = true
    setLoading(true)
    setError('')

    const requestedAt = Date.now()

    try {
      const data = await apiRequest('/api/auth/login', {
        method: 'POST',
        body: {
          username: username.trim(),
          password,
        },
      })

      if (
        typeof data.accessToken !== 'string' ||
        !data.accessToken ||
        data.tokenType !== 'Bearer' ||
        !Number.isFinite(data.expiresIn) ||
        data.expiresIn <= 0 ||
        typeof data.user?.id !== 'string' ||
        typeof data.user?.username !== 'string'
      ) {
        throw new Error('Dữ liệu đăng nhập không hợp lệ.')
      }

      setPassword('')

      onLogin({
        accessToken: data.accessToken,
        user: data.user,
        expiresAt: requestedAt + data.expiresIn * 1000,
      })
    } catch (err) {
      setError(err.message)
    } finally {
      submitting.current = false
      setLoading(false)
    }
  }

  return (
    <section className="card" aria-labelledby="login-title">
      <p className="eyebrow">CHÀO MỪNG TRỞ LẠI</p>
      <h1 id="login-title">Đăng nhập ví</h1>

      <p className="description">
        Xem số dư và quản lý ví demo của bạn.
      </p>

      {notice && (
        <p className="notice" role="status">
          {notice}
        </p>
      )}

      <form onSubmit={handleSubmit} aria-busy={loading}>
        <label htmlFor="username">Tên đăng nhập</label>
        <input
          id="username"
          name="username"
          type="text"
          autoComplete="username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          disabled={loading}
          required
        />

        <label htmlFor="password">Mật khẩu</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          disabled={loading}
          required
        />

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <button type="submit" disabled={loading}>
          {loading ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </button>
      </form>

      <p className="footnote">
        Bản mô phỏng học tập, không sử dụng tiền thật.
      </p>
    </section>
  )
}

// ==================== MÀN HÌNH VÍ ====================

function WalletPage({ session, onLogout }) {
  const [wallet, setWallet] = useState(null)
  const [amount, setAmount] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState('')
  const [uncertain, setUncertain] = useState(false)
  const [checked, setChecked] = useState(false)
  const [attempt, setAttempt] = useState(0)

  // Chặn hai lần bấm liên tiếp trước khi React cập nhật giao diện.
  const requestLock = useRef(false)

  useEffect(() => {
    const controller = new AbortController()

    async function loadBalance() {
      try {
        const data = await apiRequest('/api/wallet/balance', {
          token: session.accessToken,
          signal: controller.signal,
        })

        if (!controller.signal.aborted) {
          setWallet(data)
        }
      } catch (err) {
        if (controller.signal.aborted) return

        if (err.status === 401) {
          onLogout(
            'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.',
          )
          return
        }

        setError(err.message)
      }
    }

    loadBalance()

    return () => controller.abort()
  }, [session.accessToken, onLogout, attempt])

  function retryInitialLoad() {
    setError('')
    setAttempt((value) => value + 1)
  }

  async function refreshBalance() {
    if (requestLock.current) return

    requestLock.current = true
    setBusy('refresh')
    setError('')
    setMessage('')

    try {
      const data = await apiRequest('/api/wallet/balance', {
        token: session.accessToken,
      })

      setWallet(data)
      setChecked(true)
      setMessage('Đã tải số dư mới nhất từ máy chủ.')
    } catch (err) {
      if (err.status === 401) {
        onLogout(
          'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.',
        )
        return
      }

      setError(err.message)
    } finally {
      requestLock.current = false
      setBusy('')
    }
  }

  async function handleMoney(action) {
    if (requestLock.current || uncertain || !wallet) return

    setError('')
    setMessage('')

    const text = amount.trim()
    const value = Number(text)

    if (
      !/^[0-9]+$/.test(text) ||
      !Number.isSafeInteger(value) ||
      value < 1 ||
      value > 1_000_000_000
    ) {
      setError(
        'Nhập số nguyên từ 1 đến 1.000.000.000 VND, không dùng dấu phân cách.',
      )
      return
    }

    requestLock.current = true
    setBusy(action)

    try {
      // Chỉ gửi POST một lần, không tự gửi lại khi gặp lỗi.
      const data = await apiRequest(`/api/wallet/${action}`, {
        method: 'POST',
        token: session.accessToken,
        body: { amount: value },
      })

      const expectedType =
        action === 'deposit' ? 'DEPOSIT' : 'WITHDRAW'

      if (
        !Number.isSafeInteger(data.balance) ||
        data.balance < 0 ||
        data.balance > 1_000_000_000_000 ||
        data.currency !== 'VND' ||
        data.type !== expectedType ||
        data.amount !== value ||
        typeof data.transactionId !== 'string' ||
        !data.transactionId
      ) {
        throw new Error('Phản hồi giao dịch không hợp lệ.')
      }

      // Số dư luôn lấy từ response backend.
      setWallet((current) => ({
        ...current,
        balance: data.balance,
        currency: data.currency,
      }))

      setAmount('')
      setMessage(
        `${action === 'deposit' ? 'Cộng' : 'Trừ'} ` +
          `${formatMoney(value)} thành công. ` +
          `Mã giao dịch: ${data.transactionId}`,
      )
    } catch (err) {
      if (err.status === 401) {
        onLogout(
          'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.',
        )
        return
      }

      // Backend từ chối thao tác theo hợp đồng API.
      if ([400, 404, 409].includes(err.status)) {
        setError(err.message)
        return
      }

      // Không thể kết luận giao dịch thất bại nếu mất phản hồi.
      setUncertain(true)
      setChecked(false)
      setError(
        'Chưa xác định được kết quả giao dịch. Không gửi lại thao tác vừa rồi. ' +
          'Hãy kiểm tra số dư trước khi tiếp tục.',
      )
    } finally {
      requestLock.current = false
      setBusy('')
    }
  }

  function acknowledgeCheck() {
    setUncertain(false)
    setChecked(false)
    setAmount('')
    setError('')
    setMessage(
      'Bạn đã kiểm tra số dư. Việc này không xác nhận riêng kết quả ' +
        'giao dịch trước; tránh gửi lại thao tác đó nếu vẫn chưa rõ.',
    )
  }

  return (
    <section className="card" aria-labelledby="wallet-title">
      <p className="eyebrow">VÍ CỦA BẠN</p>

      <h1 id="wallet-title">
        Xin chào, {session.user.username}
      </h1>

      {!wallet && !error && (
        <p role="status">Đang tải số dư…</p>
      )}

      {wallet && (
        <>
          <div className="balance-panel" aria-live="polite">
            <p>Số dư hiện tại</p>
            <strong className="balance">
              {formatMoney(wallet.balance)}
            </strong>
          </div>

          <form
            className="money-form"
            onSubmit={(event) => event.preventDefault()}
            aria-busy={Boolean(busy)}
          >
            <label htmlFor="amount">Số tiền (VND)</label>

            <input
              id="amount"
              name="amount"
              type="text"
              inputMode="numeric"
              autoComplete="off"
              placeholder="Ví dụ: 10000"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              disabled={Boolean(busy) || uncertain}
              aria-describedby="amount-help"
            />

            <p id="amount-help" className="amount-help">
              Từ 1 đến 1.000.000.000 đồng mỗi thao tác.
              Nhập số liền nhau, không dùng dấu chấm hoặc dấu phẩy.
            </p>

            <div className="money-actions">
              <button
                type="button"
                disabled={Boolean(busy) || uncertain}
                onClick={() => handleMoney('deposit')}
              >
                {busy === 'deposit' ? 'Đang cộng…' : '+ Cộng tiền'}
              </button>

              <button
                type="button"
                className="withdraw-button"
                disabled={Boolean(busy) || uncertain}
                onClick={() => handleMoney('withdraw')}
              >
                {busy === 'withdraw' ? 'Đang trừ…' : '− Trừ tiền'}
              </button>
            </div>
          </form>
        </>
      )}

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}

      {message && (
        <p className="notice" role="status">
          {message}
        </p>
      )}

      {!wallet && error && (
        <button type="button" onClick={retryInitialLoad}>
          Thử tải lại số dư
        </button>
      )}

      {wallet && (
        <button
          className="secondary"
          type="button"
          disabled={Boolean(busy)}
          onClick={refreshBalance}
        >
          {busy === 'refresh' ? 'Đang tải…' : 'Kiểm tra số dư'}
        </button>
      )}

      {uncertain && checked && (
        <button
          className="secondary"
          type="button"
          disabled={Boolean(busy)}
          onClick={acknowledgeCheck}
        >
          Tôi đã kiểm tra số dư
        </button>
      )}

      <button
        className="secondary"
        type="button"
        disabled={Boolean(busy)}
        onClick={() => onLogout('Bạn đã đăng xuất.')}
      >
        Đăng xuất
      </button>
    </section>
  )
}

// ==================== ỨNG DỤNG CHÍNH ====================

export default function App() {
  // Token chỉ lưu trong memory; F5 sẽ yêu cầu đăng nhập lại.
  const [session, setSession] = useState(null)
  const [notice, setNotice] = useState('')

  const logout = useCallback((message = '') => {
    setSession(null)
    setNotice(message)
  }, [])

  useEffect(() => {
    if (!session) return

    function expire() {
      logout('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
    }

    const timer = window.setTimeout(
      expire,
      Math.max(0, session.expiresAt - Date.now()),
    )

    // Kiểm tra lại khi quay về tab hoặc máy vừa thức dậy.
    function checkExpiry() {
      if (Date.now() >= session.expiresAt) {
        expire()
      }
    }

    window.addEventListener('focus', checkExpiry)
    document.addEventListener('visibilitychange', checkExpiry)

    return () => {
      window.clearTimeout(timer)
      window.removeEventListener('focus', checkExpiry)
      document.removeEventListener('visibilitychange', checkExpiry)
    }
  }, [session, logout])

  function handleLogin(nextSession) {
    setNotice('')
    setSession(nextSession)
  }

  return (
    <main
      className={`app-shell ${
        session ? 'wallet-screen' : 'login-screen'
      }`}
    >
      <p className="brand">Tommy Wallet</p>

      {session ? (
        <div className="wallet-frame">
          <WalletPage session={session} onLogout={logout} />
        </div>
      ) : (
        <div className="login-frame">
          <section
            className="login-intro"
            aria-labelledby="intro-title"
          >
            <p className="eyebrow">TOMMY WALLET</p>

            <h2 id="intro-title">
              Ví của bạn.
              <br />
              Trong tầm tay.
            </h2>

            <p className="intro-description">
              Đăng nhập để xem số dư và trải nghiệm ví tiền mô phỏng.
            </p>

            <span className="demo-badge">
              Không sử dụng tiền thật
            </span>
          </section>

          <div className="login-form-area">
            <LoginForm
              onLogin={handleLogin}
              notice={notice}
            />
          </div>
        </div>
      )}
    </main>
  )
}