<template>
  <div id="admin-div">
    <button v-on:click="logout">Logout</button>
    <h1>Statistics:</h1>
    <p v-bind:class="statsLoaded() ? 'hidden' : ''">{{ statistics }}</p>
    <div v-bind:class="statsLoaded() ? '' : 'hidden'">
      <div>Number of users: {{statistics.numberOfUsers}}</div>
      <table>
        <thead>
          <tr>
            <td>Query</td><td>Result</td>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in statistics.lastSearches" v-bind:key="index">
            <td>{{item.request.query}}</td>
            <td>{{item.result}}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script lang="ts">
import Component from "vue-class-component";
import Vue from "vue";
import {AdminApi, SearchStatistics} from "fsearch_client";
import {clientConfig} from "@/main";

enum NoStats {
  Loading = 'Loading statistics', Failed = 'Failed to load statistics'
}

@Component({})
export default class AdminComponent extends Vue {

  private client = new AdminApi(clientConfig);

  statistics: SearchStatistics | NoStats = NoStats.Loading

  statsLoaded(): boolean {
    return this.statistics instanceof SearchStatistics
  }

  created() {
    this.fetchInfo()
  }

  fetchInfo() {
    this.client.searchStats().then((stats) => {
      this.statistics = stats
    }, () => {
      this.statistics = NoStats.Failed
    })
  }

  logout() {
    this.$store.commit('login', null)
    this.$router.push("/login")
  }

}
</script>

<style scoped>
#admin-div {
  padding-top: 50px;
  max-width: 100%;
  margin: auto;
  text-align: center;
}

#admin-div>span {
  font-size: 30px;
  font-weight: bold;
  color: #8bbeff;
}
</style>