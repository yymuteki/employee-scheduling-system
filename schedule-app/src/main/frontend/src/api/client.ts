import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.response.use(
  (res) => res,
  (err) => {
    // Don't redirect — App.tsx handles unauthenticated state by showing LoginPage
    return Promise.reject(err)
  }
)

export default client
