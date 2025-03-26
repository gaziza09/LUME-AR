<template>
  <RouterView></RouterView>
  <SosToast ref="sosToast" />
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import SosToast from './components/SosToast.vue'

const sosToast = ref(null)
let wsConnection = null

const setupWebSocket = () => {
  wsConnection = new WebSocket('wss://easywork.kz/ws_sos/')
  
  wsConnection.onopen = () => {
    console.log('WebSocket connected for SOS monitoring')
  }
  
  wsConnection.onmessage = (event) => {
    const data = JSON.parse(event.data)
    
    if (data.new_sos_requests && data.new_sos_requests.length > 0) {
      // Показываем уведомление для каждой новой заявки
      data.new_sos_requests.forEach(request => {
        const message = `New SOS request received at ${request.human_readable_time}`
        sosToast.value?.showToast(message)
      })
    }
  }
  
  wsConnection.onerror = (error) => {
    console.error('WebSocket error:', error)
  }
  
  wsConnection.onclose = () => {
    console.log('WebSocket closed, attempting to reconnect...')
    setTimeout(setupWebSocket, 5000)
  }
}

onMounted(() => {
  setupWebSocket()
})

onUnmounted(() => {
  if (wsConnection) {
    wsConnection.close()
  }
})
</script>

<style>
/* Глобальные стили */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: Arial, sans-serif;
  line-height: 1.6;
}
</style>