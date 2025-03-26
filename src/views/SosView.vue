<template>
  <div class="vosk-client">
    <header class="animate__animated animate__fadeInDown">
      <h1><span class="lume">Lume</span><span class="ar">Ar</span></h1>
      <p class="subtitle">SOS Requests Monitor</p>
    </header>

    <div class="main-content" style="display: flow-root;">
      <!-- Navigation Buttons -->
      <div class="button-group animate__animated animate__fadeIn">
        <button @click="goToHome" class="btn secondary animate__animated animate__bounceIn">
          Back to Home
        </button>
        <button @click="goToAbout" class="btn secondary animate__animated animate__bounceIn">
          Nature sounds
        </button>
      </div>

      <!-- SOS Requests Section -->
      <div class="sos-section card animate__animated animate__fadeIn">
        <h2>SOS Requests</h2>
        <div class="sos-list">
          <div v-for="request in sosRequests" :key="request.timestamp" class="sos-card animate__animated animate__fadeInUp">
            <div class="sos-header">
              <span class="sos-icon">🚨</span>
              <span class="sos-message">{{ request.message }}</span>
            </div>
            <div class="sos-time">{{ request.human_readable_time }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- GitHub Link -->
    <footer class="github-link animate__animated animate__fadeInUp animate__delay-1s">
      <a href="https://github.com/gaziza09/LUME-AR" target="_blank">Check out on GitHub</a>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const sosRequests = ref([])
let wsConnection = null
let lastTimestamp = 0

// Функция для запроса разрешения на push-уведомления
const requestNotificationPermission = async () => {
  try {
    const permission = await Notification.requestPermission()
    if (permission === 'granted') {
      console.log('Push notifications permission granted')
    }
  } catch (error) {
    console.error('Error requesting notification permission:', error)
  }
}

// Функция для показа push-уведомления
const showNotification = (message, time) => {
  if (Notification.permission === 'granted') {
    new Notification('SOS Alert!', {
      body: `New SOS request received at ${time}`,
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      vibrate: [200, 100, 200],
      tag: 'sos-notification'
    })
  }
}

// Функция для проверки новых SOS-запросов
const checkNewSosRequests = async () => {
  try {
    const response = await fetch('https://easywork.kz/get_sos/')
    const data = await response.json()
    
    // Проверяем новые запросы
    const newRequests = data.sos_requests.filter(request => request.timestamp > lastTimestamp)
    
    if (newRequests.length > 0) {
      // Обновляем последний timestamp
      lastTimestamp = Math.max(...data.sos_requests.map(r => r.timestamp))
      
      // Показываем уведомления для новых запросов
      newRequests.forEach(request => {
        showNotification(request.message, request.human_readable_time)
      })
    }
    
    // Обновляем список запросов
    sosRequests.value = data.sos_requests
  } catch (error) {
    console.error('Error fetching SOS requests:', error)
  }
}

// WebSocket подключение для real-time обновлений
const setupWebSocket = () => {
  wsConnection = new WebSocket('wss://easywork.kz/ws/sos')
  
  wsConnection.onopen = () => {
    console.log('WebSocket connected for SOS monitoring')
  }
  
  wsConnection.onmessage = (event) => {
    const data = JSON.parse(event.data)
    if (data.type === 'sos') {
      checkNewSosRequests()
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

const goToHome = () => {
  router.push('/')
}

const goToAbout = () => {
  router.push('/about')
}

onMounted(async () => {
  // Запрашиваем разрешение на уведомления
  await requestNotificationPermission()
  
  // Настраиваем WebSocket
  setupWebSocket()
  
  // Делаем первичный запрос данных
  await checkNewSosRequests()
  
  // Устанавливаем интервал для проверки новых запросов
  const interval = setInterval(checkNewSosRequests, 30000)
  
  // Сохраняем интервал для очистки при размонтировании
  onUnmounted(() => {
    clearInterval(interval)
    if (wsConnection) {
      wsConnection.close()
    }
  })
})
</script>

<style scoped>
.vosk-client {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 1rem;
}

header {
  text-align: center;
  margin-bottom: 1.5rem;
  padding: 0 1rem;
}

h1 {
  font-size: clamp(1.8rem, 5vw, 2.5rem);
  margin: 0;
  padding: 0;
}

.lume {
  color: #2c3e50;
}

.ar {
  color: #3498db;
}

.subtitle {
  color: #7f8c8d;
  font-size: clamp(1rem, 3vw, 1.2rem);
  margin-top: 0.5rem;
}

.main-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 1rem;
}

.button-group {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

.btn {
  padding: clamp(0.6rem, 2vw, 0.8rem) clamp(1rem, 3vw, 1.5rem);
  border: none;
  border-radius: 8px;
  font-size: clamp(0.9rem, 2.5vw, 1rem);
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.btn.secondary {
  background: #3498db;
  color: white;
}

.btn.secondary:hover {
  background: #2980b9;
  transform: translateY(-2px);
}

.card {
  background: white;
  border-radius: 15px;
  padding: clamp(1rem, 3vw, 2rem);
  margin-bottom: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.sos-section h2 {
  color: #2c3e50;
  margin-bottom: 1.5rem;
  text-align: center;
  font-size: clamp(1.2rem, 4vw, 1.8rem);
}

.sos-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
  width: 100%;
}

.sos-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: clamp(1rem, 2vw, 1.5rem);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.sos-card:hover {
  transform: translateY(-2px);
}

.sos-header {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  margin-bottom: 0.5rem;
}

.sos-icon {
  font-size: clamp(1.2rem, 3vw, 1.5rem);
}

.sos-message {
  font-size: clamp(1rem, 2.5vw, 1.2rem);
  font-weight: 600;
  color: #e74c3c;
}

.sos-time {
  color: #7f8c8d;
  font-size: clamp(0.8rem, 2vw, 0.9rem);
}

.github-link {
  text-align: center;
  margin-top: 2rem;
  padding: 0 1rem;
}

.github-link a {
  color: #2c3e50;
  text-decoration: none;
  font-size: clamp(0.8rem, 2vw, 0.9rem);
}

.github-link a:hover {
  color: #3498db;
}

/* Медиа-запросы для дополнительной адаптивности */
@media (max-width: 480px) {
  .vosk-client {
    padding: 0.5rem;
  }

  .main-content {
    padding: 0 0.5rem;
  }

  .card {
    padding: 1rem;
  }

  .sos-list {
    grid-template-columns: 1fr;
  }

  .button-group {
    flex-direction: column;
    align-items: stretch;
    margin-bottom: 1.5rem;
  }

  .btn {
    width: 100%;
  }
}

@media (min-width: 481px) and (max-width: 768px) {
  .sos-list {
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  }
}

@media (min-width: 769px) {
  .sos-list {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }
}
</style> 