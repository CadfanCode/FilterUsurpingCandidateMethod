<script setup>
import { ref, onMounted } from 'vue'

const data = ref(null)

onMounted(async () => {
  try {
    // Using the environment variable!
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/test`)
    data.value = await response.json()
  } catch (error) {
    console.error("Error fetching data:", error)
  }
})
</script>

<template>
  <div>
    <h1>Backend Response:</h1>
    <pre>{{ data }}</pre>
  </div>
</template>