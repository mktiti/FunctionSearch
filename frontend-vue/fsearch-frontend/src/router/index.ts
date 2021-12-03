import Vue from 'vue'
import VueRouter, { RouteConfig } from 'vue-router'
import Search from '../views/Search.vue'
import Login from '../views/Login.vue'
import Todo from "@/views/Todo.vue";

Vue.use(VueRouter)

const routes: Array<RouteConfig> = [
  {
    path: '/',
    name: 'Search',
    component: Search
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
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

export default router
