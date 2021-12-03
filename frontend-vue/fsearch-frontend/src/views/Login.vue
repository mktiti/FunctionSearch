<template>
  <div id="login-panel">
    <div id="login-message">{{message}}</div>
    <label for="username-in">Username:</label>
    <input id="username-in" v-model="username" type="text" v-on:keyup.enter="login">
    <label for="password-in">Password:</label>
    <input id="password-in" v-model="password" type="password" v-on:keyup.enter="login">
    <button v-on:click="login">Login</button>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from "vue-class-component"
import {Configuration} from "fsearch_client";
import {clientConfig} from "@/main";

class LoginApi {
  constructor(unused: Configuration) {
    console.info(unused)
  }

  login(request: LoginRequest): Promise<boolean> {
    const result: boolean = request.username === request.password
    return new Promise(resolve => setTimeout(resolve, 1000)).then(() => result);
  }
}

class LoginRequest {
  username: string;
  password: string;

  constructor(username: string, password: string) {
    this.username = username;
    this.password = password;
  }
}

@Component({})
export default class LoginComponent extends Vue {
  private client = new LoginApi(clientConfig);

  public message = ''
  public username = ''
  public password = ''

  login(): void {
    this.message = ''
    const reqData: LoginRequest = {
      username: this.username,
      password: this.password
    }

    this.client.login(reqData).then(res => {
      if (res) {
        this.$router.push('/')
      } else {
        this.message = 'Invalid username or password'
      }
    })
  }

}
</script>

<style scoped>
</style>
