import Vue from 'vue'
import VueRouter, {RouteConfig} from 'vue-router'
import Search from '../views/Search.vue'
import Login from '../views/Login.vue'
import Todo from "@/views/Todo.vue";
import Profile from "@/views/Profile.vue";
import Admin from "@/views/Admin.vue";
import {Role, User} from "@/service/AuthService";
import {store} from "@/service/Store";

Vue.use(VueRouter)

enum LoginReq {
  Any,
  LoginForbidden,
  AnyLoginRequired,
  UserLoginRequired,
  AdminLoginRequired
}

function accepted(req: LoginReq, user: User | null): boolean {
  switch (req) {
    case LoginReq.Any: return true
    case LoginReq.LoginForbidden: return user === null
    case LoginReq.AnyLoginRequired: return user !== null
    case LoginReq.UserLoginRequired: return user?.role == Role.User
    case LoginReq.AdminLoginRequired: return user?.role == Role.Admin
  }

  return false
}

const routes: Array<RouteConfig> = [
  {
    path: '/',
    name: 'Search',
    component: Search
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: {
      loginReq: LoginReq.LoginForbidden
    }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: {
      loginReq: LoginReq.UserLoginRequired
    }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: Admin,
    meta: {
      loginReq: LoginReq.AdminLoginRequired
    }
  },
  {
    path: '/demos',
    name: 'Demos',
    component: Todo
  }
]

const router = new VueRouter({
  routes,
  mode: 'history'
})

router.beforeEach((to, from, next) => {
  if (to.meta.loginReq && !accepted(to.meta.loginReq, store.state.loggedInUser)) {
      next(from.path)
  } else {
    next()
  }
})

export default router
