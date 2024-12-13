<script setup lang="ts">

import {useTeamsheetStore} from "@/utils/teamsheet_store";
import {useRouter} from "vue-router";

const store = useTeamsheetStore()
const router = useRouter()

const targetUrl = "/edit/" + store.uploadSuccessDEO?.fileKey

</script>

<template>
Thank you for submitting your teamsheet. Your submission has been received.<br>
  If you wish to edit the submitted date up until the day of the tournament, you can do so using the following link:
  <router-link :to="targetUrl">Edit Teamsheet</router-link>
</template>


<style scoped>

</style>
