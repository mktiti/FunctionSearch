<template>
  <div v-if="isInited()">
    <div v-if="isLoading()">
      <div class="message loading">
        Processing query
      </div>
    </div>
    <div v-else-if="isSuccess()">
      <div class="message">
        Search results
      </div>
      <ul id="results">
        <li v-for="(item, index) in result.results.results" v-bind:key="index">
          <ResultEntry :entry="item"></ResultEntry>
        </li>
      </ul>
      <div id="res-trim-message" v-if="result.results.trimmed">Some results omitted</div>
    </div>
    <div v-else>
      <div class="message error">
        <span v-if="isInternalError()">Internal error while processing query</span>
        <span v-else>Invalid query</span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Component from "vue-class-component";
import Vue from "vue";
import {QueryResult} from "fsearch_client";
import {isInternalError, isQueryError, isSuccess} from "@/util/Result";
import ResultEntry from "@/components/ResultEntry.vue";
import {QueryState} from "@/util/QueryState";

const ResultBase = Vue.extend({
  props: {
    result: {
      type: Object as () => QueryResult | null
    },
    state: {
      type: Number
    }
  }
})

@Component({
  components: {ResultEntry}
})
export default class SearchResults extends ResultBase {
  isInited(): boolean {
    return this.state !== QueryState.None;
  }

  isLoading(): boolean {
    return this.state === QueryState.Loading;
  }

  isSuccess(): boolean {
    return isSuccess(this.result);
  }

  isQueryError(): boolean {
    return isQueryError(this.result);
  }

  isInternalError(): boolean {
    return isInternalError(this.result);
  }
}
</script>

<style scoped>
#res-trim-message {
  color: #e7e7e7;
  font-size: 18px;
  font-weight: bold;
  margin: 15px;
  text-align: center;
}

.message {
  font-weight: bold;
  font-size: 20px;
  margin: 15px 0 10px 0;
  color: #e7e7e7;
}

.message.error {
  color: #dc0000;
}

.message.loading {
  color: #37a100;
}

ul {
  list-style-type: none;
  margin: 0;
  padding: 0;
}
</style>
