import axios, { AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'

const instance: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 10000,
  withCredentials: true
})

// Request interceptor
instance.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
instance.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // Unauthorized - clear auth data and redirect to login
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          if (window.location.pathname !== '/') {
            ElMessage.error('Session expired. Please login again.')
            window.location.href = '/'
          }
          break
        case 403:
          ElMessage.error('Permission denied')
          break
        case 404:
          ElMessage.error('Resource not found')
          break
        case 500:
          ElMessage.error('Server error')
          break
        default:
          if (error.response.data?.error) {
            ElMessage.error(error.response.data.error)
          } else {
            ElMessage.error('Request failed')
          }
      }
    } else if (error.request) {
      ElMessage.error('Network error - no response from server')
    } else {
      ElMessage.error('Request configuration error')
    }
    return Promise.reject(error)
  }
)

export default instance