<template>
  <div id="search-panel">
    <SearchInput v-on:search="initSearch"></SearchInput>
    <SearchResults :result="result" :state="queryState"></SearchResults>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from "vue-class-component"
import SearchInput from "@/components/SearchInput.vue";
import {QueryCtxDto, QueryRequestDto, QueryResult, SearchService} from "@/service/generated-client";
import SearchResults from "@/components/SearchResults.vue";
import {QueryState} from "@/util/QueryState";

const defaultContext: QueryCtxDto = {
  artifacts: [
    {group: 'org.apache.commons', name: 'commons-lang3', version: '3.11'},
    {group: 'com.google.guava', name: 'guava', version: '30.0-jre'}
  ]
}

@Component({
  components: {SearchInput, SearchResults}
})
export default class SearchComponent extends Vue {
  private result: QueryResult | null = null;
  private queryState: QueryState = QueryState.None;

  initSearch(query: string): void {
    const reqData: QueryRequestDto = {
      query: query,
      context: defaultContext
    }
    this.result = null
    this.queryState = QueryState.Loading
    SearchService.syncQuery(reqData).then(res => {
      this.result = res
      this.queryState = QueryState.Done
    });
  }
}
</script>

<style scoped>
#search-panel {
  padding-top: 30px;
  width: 700px;
  max-width: 100%;
  margin: auto;
}
</style>
