<template>
  <div v-if="show" class="toast animate__animated animate__fadeInRight">
    <div class="toast-content">
      <span class="toast-icon">🚨</span>
      <div class="toast-message">
        <h4>New SOS Request!</h4>
        <p>{{ message }}</p>
      </div>
    </div>
    <div class="toast-actions">
      <button @click="goToSos" class="toast-btn">View SOS Monitor</button>
      <button @click="close" class="toast-close">×</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const show = ref(false)
const message = ref('')

const goToSos = () => {
  router.push('/sos')
  close()
}

const close = () => {
  show.value = false
}

const showToast = (msg) => {
  message.value = msg
  show.value = true
  // Автоматически скрываем через 10 секунд
  setTimeout(close, 10000)
}

// Экспортируем метод для использования извне
defineExpose({ showToast })
</script>

<style scoped>
.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: white;
  border-radius: 8px;
  padding: 1rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  min-width: 300px;
  max-width: 400px;
}

.toast-content {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}

.toast-icon {
  font-size: 1.5rem;
}

.toast-message h4 {
  margin: 0;
  color: #e74c3c;
  font-size: 1.1rem;
}

.toast-message p {
  margin: 0.5rem 0 0;
  color: #2c3e50;
  font-size: 0.9rem;
}

.toast-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toast-btn {
  background: #3498db;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.3s ease;
}

.toast-btn:hover {
  background: #2980b9;
}

.toast-close {
  background: none;
  border: none;
  color: #7f8c8d;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0 0.5rem;
  transition: color 0.3s ease;
}

.toast-close:hover {
  color: #2c3e50;
}

@media (max-width: 480px) {
  .toast {
    left: 20px;
    right: 20px;
    min-width: auto;
  }
}
</style> 