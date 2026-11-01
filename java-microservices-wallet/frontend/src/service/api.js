export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export async function apiRequest(
  path,
  { method = 'GET', body, token, signal } = {},
) {
  let response

  try {
    response = await fetch(path, {
      method,
      signal,
      cache: 'no-store',
      headers: {
        Accept: 'application/json',
        ...(body !== undefined
          ? { 'Content-Type': 'application/json' }
          : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error

    throw new ApiError(
      'Không kết nối được máy chủ. Vui lòng kiểm tra kết nối.',
      0,
      'NETWORK_ERROR',
    )
  }

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    throw new ApiError(
      data?.message || 'Không thực hiện được yêu cầu. Vui lòng thử lại sau.',
      response.status,
      data?.code || 'REQUEST_FAILED',
    )
  }

  if (data === null) {
    throw new ApiError(
      'Máy chủ trả về dữ liệu không hợp lệ.',
      response.status,
      'INVALID_RESPONSE',
    )
  }

  return data
}