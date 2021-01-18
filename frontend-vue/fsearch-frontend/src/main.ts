import Vue from 'vue'
import App from './App.vue'
import router from './router'
import { library } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faSearch } from '@fortawesome/free-solid-svg-icons'
import { faLinkedin, faGithub } from '@fortawesome/free-brands-svg-icons'
import {OpenAPI, SearchService} from "@/service/generated-client";

library.add(faSearch, faLinkedin, faGithub)
Vue.component('fa-icon', FontAwesomeIcon)

Vue.config.productionTip = false

OpenAPI.BASE = 'http://localhost:8080/api/v1'

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
