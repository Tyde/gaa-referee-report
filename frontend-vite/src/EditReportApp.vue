<script setup lang="ts">
import TeamSelector from './components/TeamSelector.vue'
import {ref} from "vue";
import type Team from "@/types";
const selected_teams = ref(<Team[]>[])

function submit_teams(teams_added:Array<Team>) {
  selected_teams.value.length = 0;
  selected_teams.value= selected_teams.value.concat(teams_added)
}
</script>

<template>
  <header>


    <div class="p-card">
      Selected Teams in App:
      <table>
        <tr v-for="teams in selected_teams">
          <td>{{teams.name}}</td>
        </tr>
      </table>
    </div>
  </header>

  <main>
    <TeamSelector
     @submit-teams="submit_teams"/>
  </main>

</template>

<style>

</style>
