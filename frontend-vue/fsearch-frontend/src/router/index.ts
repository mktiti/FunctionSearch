import Vue from 'vue'
import VueRouter, { RouteConfig } from 'vue-router'
import Search from '../views/Search.vue'

Vue.use(VueRouter)

const routes: Array<RouteConfig> = [
  {
    path: '/',
    name: 'Search',
    component: Search
  }
]

const router = new VueRouter({
  routes,
  mode: 'history'
})

export default router
