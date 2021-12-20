<template>
  <div id="profile-div">
    <p>Welcome back, {{$store.getters.loggedUsername}}!</p>
    <div>{{ this.infoString() }}</div>
    <button v-on:click="logout">Logout</button>
  </div>
</template>

<script lang="ts">
import Component from "vue-class-component";
import Vue from "vue";
import {UserApi, UserInfo} from "fsearch_client";
import {clientConfig} from "@/main";
import {capitalCase} from "@/util/string";

enum NoUser {
  Loading, Failed
}

@Component({})
export default class ProfileComponent extends Vue {

  private client = new UserApi(clientConfig);

  info: UserInfo | NoUser = NoUser.Loading

  created() {
    this.fetchInfo()
  }

  infoString(): string {
    if (this.info instanceof UserInfo) {
      const levelString = capitalCase(this.info.level)
      return `${levelString} level member, since ${this.info.registerDate}`
    } else {
      switch (this.info) {
        case NoUser.Loading: return "Loading user info"
        case NoUser.Failed: return "Failed to load user info"
      }
    }
  }

  fetchInfo() {
    this.client.selfData().then((user) => {
      this.info = user
    }, () => {
      this.info = NoUser.Failed
    })
  }

  logout() {
    this.$store.commit('login', null)
    this.$router.push("/login")
  }

}
</script>

<style scoped>
#profile-div {
  padding-top: 50px;
  max-width: 100%;
  margin: auto;
  text-align: center;
  font-size: 30px;
  font-weight: bold;
}
#profile-div>p {
  display: block;
  color: #8bbeff;
}
button {
  background: #1d89ff;
  border: none;
  font-size: larger;
  color: white;

  font-weight: 600;
  letter-spacing: 1px;
  padding: 5px 10px;
  cursor: pointer;
}
button:hover, button:active {
  background: #476a9a;
}
</style>