import { createContext, useContext, useState, type ReactNode } from 'react'
import type { UserVO } from '../types/api'

interface UserContextValue {
  user: UserVO | null
  setUser: (user: UserVO | null) => void
  // 初始化加载状态（用于首次拉取 current 时显示 loading）
  initializing: boolean
  setInitializing: (v: boolean) => void
}

const UserContext = createContext<UserContextValue>({
  user: null,
  setUser: () => {},
  initializing: true,
  setInitializing: () => {},
})

export const UserProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<UserVO | null>(null)
  const [initializing, setInitializing] = useState(true)
  return (
    <UserContext.Provider value={{ user, setUser, initializing, setInitializing }}>
      {children}
    </UserContext.Provider>
  )
}

export const useUser = () => useContext(UserContext)
