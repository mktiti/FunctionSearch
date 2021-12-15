<template>
  <div id="profile-div">
    <p>Welcome back, {{$store.getters.loggedUsername}}!</p>
    <button v-on:click="logout">Logout</button>
  </div>
</template>

<script lang="ts">
import Component from "vue-class-component";
import Vue from "vue";

@Component({})
export default class ProfileComponent extends Vue {

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