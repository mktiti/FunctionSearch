<template>
  <div id="search-bar">
    <input class="code-in" maxlength="256"
           type="search" autocomplete="off" autofocus
           placeholder="Search for functions" title="Search for function"
           v-model="query" v-on:keyup.enter="initSearch" >

    <button id="search-submit" v-on:click="initSearch" aria-label="Search" title="Search">
      <fa-icon icon="search" />
    </button>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from "vue-class-component"

@Component
export default class SearchInput extends Vue {
  public query = ''

  initSearch(): void {
    this.$emit('search', this.query)
  }
}
</script>

<style scoped>
#search-bar {
  position: relative;
  display: flex;
  max-height: 48px;
  margin: 10px auto;
}

#search-bar>input {
  padding-inline-start: 15px;
  padding-inline-end: 48px;
  width: 100%;
  height: 48px;

  font-size: 15px;
  background: #38383d;
  color: #f9f9fa;
  font-family: monospace;
  border: solid 1px #5f5f63;
  border-radius: 3px;
}

#search-bar>input:active, #search-bar>input:focus, #search-bar>input:focus-within {
  border: 1px solid #45a1ff;
  box-shadow: 0 0 2px 3px #324e6d;
}

#search-submit {
  position: absolute;
  height: 100%;
  width: 48px;
  inset-inline-end: 0;
  background: none;
  background-size: 16px 16px;
  color: #ababae;
  border: 0;
  border-radius: 0 3px 3px 0;
}

#search-submit:hover {
  background-color: rgba(12, 12, 13, 0.15);
  cursor: pointer;
}
</style>
