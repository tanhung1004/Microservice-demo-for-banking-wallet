import { useCallback, useEffect, useRef, useState } from 'react'
import { apiRequest } from './service/api.js'
import './App.css'

// Form đăng nhập
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

// Màn hình ví sau khi đăng nhập
function WalletPage({ session, onLogout }) {
  const [wallet, setWallet] = useState(null)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)

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
            'Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại.',
          )
          return
        }

        setError(err.message)
      }
    }

    loadBalance()

    return () => controller.abort()
  }, [session.accessToken, onLogout, attempt])

  function retry() {
    setError('')
    setWallet(null)
    setAttempt((value) => value + 1)
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

      {error && (
        <div>
          <p className="error" role="alert">
            {error}
          </p>
          <button type="button" onClick={retry}>
            Thử tải lại
          </button>
        </div>
      )}

      {wallet && (
        <div className="balance-panel">
          <p>Số dư hiện tại</p>

          <strong className="balance">
            {new Intl.NumberFormat('vi-VN', {
              style: 'currency',
              currency: 'VND',
              maximumFractionDigits: 0,
            }).format(wallet.balance)}
          </strong>
        </div>
      )}

      <button
        className="secondary"
        type="button"
        onClick={() => onLogout('Bạn đã đăng xuất.')}
      >
        Đăng xuất
      </button>
    </section>
  )
}

// Quản lý phiên và bố cục chung
export default function App() {
  // Token chỉ lưu trong memory; tải lại trang sẽ cần đăng nhập lại.
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