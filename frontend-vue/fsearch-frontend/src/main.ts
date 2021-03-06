import Vue from 'vue';
import App from './App.vue';
import router from './router';
import {library} from "@fortawesome/fontawesome-svg-core";
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faChevronDown, faChevronUp, faPlus, faSearch, faTrashAlt} from '@fortawesome/free-solid-svg-icons';
import {faGithub, faLinkedin} from '@fortawesome/free-brands-svg-icons';
import {createConfiguration, ServerConfiguration} from "fsearch_client";

export const server = new ServerConfiguration<{  }>(process.env.VUE_APP_API_BASE, {  })
export const clientConfig = createConfiguration({
  baseServer: server
})

library.add(faSearch, faLinkedin, faGithub, faChevronDown, faChevronUp, faTrashAlt, faPlus)
Vue.component('fa-icon', FontAwesomeIcon)

Vue.config.productionTip = false

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
