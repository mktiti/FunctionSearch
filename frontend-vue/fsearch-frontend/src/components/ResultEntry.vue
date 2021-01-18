<template>
  <div class="entry" v-on:click="opened = !opened">
    <div class="file">{{entry.file}}</div>
    <div class="header">{{entry.header}}</div>
    <div class="short-info">{{entry.doc.shortInfo}}</div>
    <div class="long-info" v-if="opened"><pre>{{entry.doc.details}}</pre></div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import Component from "vue-class-component";
import {QueryFitResult} from "@/service/generated-client";

const ResultEntryBase = Vue.extend({
  props: {
    entry: {
      type: Object as () => QueryFitResult
    }
  }
})

@Component
export default class ResultEntry extends ResultEntryBase {
  private opened = false

}
</script>

<style scoped>
.entry {
  border: solid 1px #5f5f63;
  border-radius: 3px;
  padding: 10px;
  margin-bottom: 7px;
  cursor: pointer;
  color: #e7e7e7;
}

.entry>.file, .entry>.header {
  font-family: monospace;
  font-size: 15px;
  margin-bottom: 5px;
}

.entry>.file {
  font-weight: bold;
}

.entry>.header {
  color: #abb7ff;
}

.entry>.header {
  color: #abb7ff;
}

.entry:active, .entry:hover {
  border-color: #45a1ff;
}
</style>
