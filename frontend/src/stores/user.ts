import { reactive, computed } from 'vue'

interface User {
  id: string
  username: string
  email: string
  role: string
  created_at: string
}

const state = reactive({
  user: null as User | null
})

export const useUserStore = () => {
  const loadUser = () => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      state.user = JSON.parse(storedUser)
    }
  }

  const setUser = (user: User) => {
    state.user = user
    localStorage.setItem('user', JSON.stringify(user))
  }

  const clearUser = () => {
    state.user = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  const isAdmin = computed(() => {
    return state.user?.role === 'Admin' || state.user?.role === 'ADMIN'
  })

  const currentUser = computed(() => state.user)

  // Load user on initialization
  if (!state.user) {
    loadUser()
  }

  return {
    currentUser,
    isAdmin,
    setUser,
    clearUser,
    loadUser
  }
}