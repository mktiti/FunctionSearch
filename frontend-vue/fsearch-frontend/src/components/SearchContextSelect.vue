<template>
  <div id="main">
    <div id="title" v-on:click="expanded = !expanded">
      {{title()}}
      <fa-icon icon="chevron-up" v-if="expanded" />
      <fa-icon icon="chevron-down" v-else />
    </div>
    <div id="details" v-if="expanded">
      <div v-for="(artifact, index) in context" v-bind:key="index">
        <fa-icon v-on:click="remove(artifact)" icon="trash-alt"></fa-icon>
        {{artifact.group}}:{{artifact.name}}:{{artifact.version}}
      </div>
      <div>
        <fa-icon icon="plus" v-on:click="add"></fa-icon>
        <input id="new-artifact" type="text" maxlength="100" placeholder="Artifact ID"
              v-on:keyup.enter="add" v-model="newArtifactId">
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import Component from "vue-class-component";
import ResultEntry from "@/components/ResultEntry.vue";
import {ArtifactIdDto} from "@/service/generated-client";

const SearchCtxBase = Vue.extend({
  props: {
    context: {
      type: Array,
      required: true
    }
  }
})

@Component({
  components: {ResultEntry}
})
export default class SearchContextSelect extends SearchCtxBase {
  private expanded = false
  private newArtifactId = ''

  title(): string {
    switch (this.context.length) {
      case 0: {
        return 'Search only in the standard library'
      }
      case 1: {
        return 'Search in one artifact'
      }
      default: {
        return 'Search in ' + this.context.length + ' artifacts'
      }
    }
  }

  remove(artifact: ArtifactIdDto): void {
    this.$emit('removeArtifact', artifact)
  }

  add(): void {
    const parts = this.newArtifactId.split(":")
    if (parts.length === 3) {
      const id = {
        group: parts[0].trim(),
        name: parts[1].trim(),
        version: parts[2].trim()
      }

      this.newArtifactId = ''
      this.$emit('addArtifact', id)
    } else {
      window.alert("Wrong ID format '" + this.newArtifactId + "'")
    }
  }

}
</script>

<style scoped>
#main {
  color: #f9f9fa;
  border-left: 3px solid #1d89ff;
  padding-left: 7px;
  padding-top: 4px;
}

#title {
  cursor: pointer;
  user-select: none;
}

#title svg {
  margin-left: 3px;
}

#details {
  margin-left: 30px;
  margin-top: 5px;
  margin-bottom: 5px;
  font-family: monospace;
  font-size: 16px;
}

#details>div {
  margin-top: 2px;
}

#details>div>svg {
  cursor: pointer;
}

#new-artifact {
  font-size: 15px;
  background: #38383d;
  color: #f9f9fa;
  font-family: monospace;
  border: solid 1px #5f5f63;
  border-radius: 3px;
  padding-top: 7px;
  padding-bottom: 7px;
  padding-left: 9px;
  width: 400px;
  margin-top: 5px;
  margin-bottom: 5px;
  margin-left: 10px;
}
</style>
