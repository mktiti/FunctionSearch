<template>
  <div id="login-panel">
    <div id="login-title">Login</div>
    <div id="login-controls">
      <div>
        <div id="login-message" v-bind:class="(this.message) ? '' : 'hidden'">{{message}}</div>
        <label for="username-in">Username:</label>
        <input id="username-in" v-model="username" placeholder="Username" type="text" v-on:keyup.enter="login">
        <label for="password-in">Password:</label>
        <input id="password-in" v-model="password" placeholder="Password" type="password" v-on:keyup.enter="login">
        <button v-on:click="login">Login</button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from "vue-class-component"
import {clientConfig} from "@/main";
import {AuthApi, Credentials} from "fsearch_client";
import {isLoginSuccess} from "@/util/LoginResult";

@Component({})
export default class LoginComponent extends Vue {

  private client = new AuthApi(clientConfig);

  public message = ''
  public username = ''
  public password = ''

  login(): void {
    this.message = ''
    const credentials: Credentials = {
      username: this.username,
      password: this.password
    }

    this.client.login(credentials).then(res => {
      if (isLoginSuccess(res)) {
        console.log("Logged in as " + res.username)
        this.$router.push('/')
      } else {
        this.message = 'Invalid credentials!'
      }
    })
  }

}
</script>

<style scoped>
#login-panel {
  color: white;

  position: absolute;
  left: 50%;
  top: 50%;
  -webkit-transform: translate(-50%, -50%);
  transform: translate(-50%, -50%);

  display: -webkit-flex;
  display: -ms-flexbox;
  display: flex;
  -webkit-flex-wrap: wrap;
  -ms-flex-wrap: wrap;
  flex-wrap: wrap;

  width: 80%;
}
#login-title {
  font-size: 4rem;
  font-weight: 600;
  letter-spacing: 1px;
  width: calc(50% - 20px);
  text-align: right;
  padding-right: 20px;
  align-self: center;
}
#login-controls {
  width: calc(50% - 20px - 5px);
  text-align: left;
  border-left: #8bbeff solid 5px;
  padding-left: 20px;
  padding-top: 20px;
  padding-bottom: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}
#login-controls button, #login-controls label, #login-controls input, #login-message {
  display: block;
}
#login-controls>div {
  width: 280px;
}
#login-controls input {
  width: calc(100% - 8px);
  margin-bottom: 10px;
  font-size: larger;
}
#login-controls button {
  background: #1d89ff;
  border: none;
  font-size: larger;
  color: white;

  font-weight: 600;
  letter-spacing: 1px;
  padding: 5px 10px;
  cursor: pointer;
}
#login-controls button:hover, #login-controls button:active {
  background: #476a9a;
}
#login-controls #login-message {
  background: #dc0000;
  padding: 10px 10px 7px;
  margin-bottom: 20px;
  font-weight: 600;
  letter-spacing: 1px;
  font-size: larger;
  width: calc(100% - 20px);
  display: inline-block;
  text-align: center;
}
#login-controls button {
  margin-left: auto;
  margin-right: 0;
  margin-top: 15px;
}
#login-controls #login-message.hidden {
  display: none;
}
@media(max-width: 600px) {
  #login-title, #login-controls {
    width: 100%;
    padding-left: 0;
    padding-right: 0;
  }
  #login-title {
    text-align: center;
  }
  #login-controls {
    border-left: 0;
    border-top: #8bbeff solid 5px;
    margin: auto;
    align-content: center;
  }
  #login-controls>div {
    margin: auto;
  }
}

</style>
