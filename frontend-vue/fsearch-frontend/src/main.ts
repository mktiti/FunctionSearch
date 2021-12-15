import Vue from 'vue';
import App from './App.vue';
import router from './router';
import {library} from "@fortawesome/fontawesome-svg-core";
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faChevronDown, faChevronUp, faPlus, faSearch, faTrashAlt} from '@fortawesome/free-solid-svg-icons';
import {faGithub, faLinkedin} from '@fortawesome/free-brands-svg-icons';
import {createConfiguration, ServerConfiguration} from "fsearch_client";
import {AppInfo} from "@/util/AppInfo";
import {AuthService} from "@/service/AuthService";
import {store} from "@/service/Store";

export const appInfo = new AppInfo(process.env.VUE_APP_VERSION, process.env.VUE_APP_BUILD_TIMESTAMP)

export const clientConfig = createConfiguration({
  baseServer: new ServerConfiguration<{  }>(process.env.VUE_APP_API_BASE, {  }),
  authMethods: {"bearer-jwt": {tokenProvider: new AuthService()}}
})

library.add(faSearch, faLinkedin, faGithub, faChevronDown, faChevronUp, faTrashAlt, faPlus)
Vue.component('fa-icon', FontAwesomeIcon)

Vue.config.productionTip = false

new Vue({
  el: '#app',
  store,
  router,
  render: h => h(App)
})

console.log(`Welcome to JvmSearch v${appInfo.version} (build ${appInfo.builtAt})`)
