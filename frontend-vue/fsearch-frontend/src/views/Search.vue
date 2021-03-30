<template>
  <div id="search-panel">
    <SearchContextSelect :context="queryContext.artifacts"
       v-on:removeArtifact="removeArtifact" v-on:addArtifact="addArtifact">
    </SearchContextSelect>
    <SearchInput v-on:search="initSearch"></SearchInput>
    <SearchResults :result="result" :state="queryState"></SearchResults>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import Component from "vue-class-component"
import SearchInput from "@/components/SearchInput.vue";
import SearchResults from "@/components/SearchResults.vue";
import SearchContextSelect from "@/components/SearchContextSelect.vue";
import {artifactEqual} from "@/util/Artifact";
import {ArtifactIdDto, QueryCtxDto, QueryRequestDto, QueryResult, SearchApi} from "fsearch_client";
import {QueryState} from "@/util/QueryState";
import {clientConfig} from "@/main";

const defaultContext: QueryCtxDto = {
  artifacts: [
    {group: 'org.apache.commons', name: 'commons-lang3', version: '3.11'},
    {group: 'com.google.guava', name: 'guava', version: '30.0-jre'}
  ], imports: [
    {packageName: 'java.lang', simpleName: '*'},
    {packageName: 'java.io', simpleName: '*'},
    {packageName: 'java.math', simpleName: '*'},
    {packageName: 'java.util', simpleName: '*'},
    {packageName: 'java.util.stream', simpleName: '*'}
  ]
}

@Component({
  components: {SearchContextSelect, SearchInput, SearchResults}
})
export default class SearchComponent extends Vue {
  private client = new SearchApi(clientConfig);

  private result: QueryResult | null = null;
  private queryState: QueryState = QueryState.None;

  private queryContext = defaultContext

  initSearch(query: string): void {
    const reqData: QueryRequestDto = {
      query: query,
      context: this.queryContext
    }
    this.result = null
    this.queryState = QueryState.Loading

    this.client.syncQuery(reqData).then(res => {
      this.result = res
      this.queryState = QueryState.Done
    });
  }

  removeArtifact(artifact: ArtifactIdDto): void {
    const index = this.queryContext.artifacts.indexOf(artifact);
    if (index > -1) {
      this.queryContext.artifacts.splice(index, 1);
    }
  }

  addArtifact(artifact: ArtifactIdDto): void {
    const index = this.queryContext.artifacts.findIndex(e => artifactEqual(artifact, e));
    if (index === -1) {
      this.queryContext.artifacts.push(artifact);
    } else {
      window.alert("Artifact already in context")
    }
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
