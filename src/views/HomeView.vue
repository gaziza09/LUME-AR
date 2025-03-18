<template>
  <div class="vosk-client">
    <header class="animate__animated animate__fadeInDown">
      <h1><span class="lume">Lume</span><span class="ar">Ar</span></h1>
      <p class="subtitle">Voice Recognition System</p>
    </header>
    
    <!-- Выбор языка -->
    <div v-if="!languageSelected" class="language-selection card animate__animated animate__fadeIn">
      <label for="language">Выберите язык:</label>
      <select v-model="selectedLanguage" id="language" class="language-select">
        <option value="KZ">Казахский (KZ)</option>
        <option value="RU">Русский (RU)</option>
        <option value="EN">Английский (EN)</option>
      </select>
      <button @click="confirmLanguage" class="btn primary animate__animated animate__pulse animate__infinite">Подтвердить</button>
    </div>

    <!-- Основной интерфейс -->
    <div v-if="languageSelected" class="main-content">
      <!-- Интерфейс записи -->
      <div class="recording-section card animate__animated animate__fadeIn">
        <div class="button-group">
          <button @click="requestPermission" class="btn record" :class="{ recording: isRecording }">
            {{ isRecording ? "Остановить запись" : "Начать запись" }}
            <span class="mic-icon" :class="{ active: isRecording }">🎙️</span>
          </button>
          <button @click="changeLanguage" class="btn secondary animate__animated animate__bounceIn">Сменить язык</button>
        </div>
        <p v-if="result" class="result-text animate__animated animate__fadeInUp"><strong>Распознанный текст:</strong> {{ result }}</p>
        <p v-if="error" class="error animate__animated animate__shakeX">{{ error }}</p>
      </div>

      <!-- Блок состояния -->
      <div class="status-section card animate__animated animate__fadeIn">
        <h2>Состояние</h2>
        <p><strong>WebSocket:</strong> {{ wsStatus }}</p>
        <p><strong>Микрофон:</strong> {{ micStatus }}</p>
        <p><strong>Язык:</strong> {{ selectedLanguage }}</p>
      </div>

      <!-- История распознавания -->
      <div class="history-section card animate__animated animate__fadeIn">
        <h2>История</h2>
        <ul v-if="history.length > 0" class="history-list">
          <li v-for="(item, index) in history" :key="index" class="history-item animate__animated animate__fadeInUp">
            {{ item }}
          </li>
        </ul>
        <p v-else class="no-history">Пока ничего не распознано</p>
      </div>

      <!-- О проекте -->
      <div class="about-section card animate__animated animate__fadeIn">
        <h2>О LumeAr</h2>
        <p>LumeAr — это инновационная система распознавания голоса, разработанная для упрощения взаимодействия с технологиями. Поддерживает несколько языков и работает в реальном времени.</p>
      </div>
    </div>

    <!-- Ссылка на GitHub -->
    <footer class="github-link animate__animated animate__fadeInUp animate__delay-1s">
      <a href="https://github.com/gaziza09/LUME-AR" target="_blank">Check out on GitHub</a>
    </footer>
  </div>
</template>

<script>
export default {
  data() {
    return {
      ws: null,
      result: "",
      isRecording: false,
      mediaRecorder: null,
      permissionDenied: false,
      error: null,
      audioContext: null,
      selectedLanguage: "KZ",
      languageSelected: false,
      history: [], // История распознанных фраз
      wsStatus: "Отключен", // Статус WebSocket
    };
  },
  computed: {
    micStatus() {
      return this.isRecording ? "Активен" : "Неактивен";
    },
  },
  methods: {
    confirmLanguage() {
      if (this.selectedLanguage) {
        this.languageSelected = true;
        console.log(`Выбран язык: ${this.selectedLanguage}`);
      }
    },

    changeLanguage() {
      this.stopRecording();
      this.languageSelected = false;
      this.result = "";
      this.error = null;
      console.log("Смена языка инициирована");
    },

    async requestPermission() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        stream.getTracks().forEach((track) => track.stop());
        this.permissionDenied = false;
        this.toggleRecording();
      } catch (error) {
        this.permissionDenied = true;
        this.error = error.message;
        console.error("Доступ к микрофону отклонён:", error);
      }
    },

    toggleRecording() {
      if (this.isRecording) {
        this.stopRecording();
      } else {
        this.audioContext = new (window.AudioContext ||
          window.webkitAudioContext ||
          window.mozAudioContext ||
          window.oAudioContext ||
          window.msAudioContext || window.webkitAudioContext)();
        this.startRecording();
      }
    },

    async startRecording() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const input = this.audioContext.createMediaStreamSource(stream);
        const processor = this.audioContext.createScriptProcessor(4096, 1, 1);

        this.ws = new WebSocket(`wss://easywork.kz/ws?lang=${this.selectedLanguage}`);
        this.ws.binaryType = "arraybuffer";

        this.ws.onopen = () => {
          this.wsStatus = "Подключен";
          console.log("WebSocket подключен");
        };

        this.ws.onmessage = (event) => {
          const data = JSON.parse(event.data);
          this.result = data.text || data.partial || "Нет распознанного текста.";
          if (data.text) {
            this.history.unshift(data.text); // Добавляем полный текст в историю
            if (this.history.length > 5) this.history.pop(); // Ограничиваем историю до 5 элементов
          }
        };

        this.ws.onerror = (error) => {
          this.wsStatus = "Ошибка";
          console.error("Ошибка WebSocket:", error);
        };

        this.ws.onclose = () => {
          this.wsStatus = "Отключен";
          console.log("WebSocket соединение закрыто.");
        };

        const downsampleBuffer = (buffer, targetSampleRate, sourceSampleRate) => {
          const ratio = sourceSampleRate / targetSampleRate;
          const length = Math.round(buffer.length / ratio);
          const result = new Int16Array(length);
          let offset = 0;
          let inputOffset = 0;

          while (offset < result.length) {
            const nextOffset = Math.round((offset + 1) * ratio);
            let accumulator = 0;
            let count = 0;

            for (let i = inputOffset; i < nextOffset && i < buffer.length; i++) {
              accumulator += buffer[i];
              count++;
            }

            result[offset] = Math.min(1, accumulator / count) * 0x7FFF;
            offset++;
            inputOffset = nextOffset;
          }

          return result;
        };

        processor.onaudioprocess = (event) => {
          const inputData = event.inputBuffer.getChannelData(0);
          const downsampled = downsampleBuffer(inputData, 16000, this.audioContext.sampleRate);
          if (this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(downsampled.buffer);
          }
        };

        input.connect(processor);
        processor.connect(this.audioContext.destination);

        this.isRecording = true;
        console.log("Запись началась...");
      } catch (error) {
        console.error("Ошибка доступа к микрофону:", error);
        this.error = error.message;
      }
    },

    stopRecording() {
      if (this.isRecording) {
        this.ws.close();
        this.audioContext.close();
        this.isRecording = false;
        console.log("Запись остановлена.");
      }
    },
  },
};
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

.vosk-client {
  max-width: 900px; /* Увеличил ширину для размещения блоков */
  margin: 40px auto;
  font-family: 'Inter', sans-serif;
  text-align: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
  border-radius: 15px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

header {
  margin-bottom: 30px;
}

h1 {
  font-size: 2.5em;
  margin: 0;
  color: #2c3e50;
}

.lume {
  color: #3498db;
  font-weight: bold;
}

.ar {
  color: #e74c3c;
  font-weight: bold;
}

.subtitle {
  font-size: 1.1em;
  color: #7f8c8d;
  margin-top: 5px;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.card {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease;
}

.card:hover {
  transform: translateY(-5px);
}

.language-selection {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

label {
  font-size: 1.2em;
  color: #34495e;
}

.language-select {
  padding: 10px;
  font-size: 1em;
  border: 1px solid #dcdcdc;
  border-radius: 5px;
  background: #fff;
  width: 200px;
  cursor: pointer;
  transition: border-color 0.3s ease;
}

.language-select:focus {
  border-color: #3498db;
  outline: none;
}

.button-group {
  display: flex;
  gap: 15px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn {
  padding: 12px 25px;
  font-size: 1em;
  border: none;
  border-radius: 25px;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.2s ease;
}

.btn.primary {
  background-color: #3498db;
  color: white;
}

.btn.primary:hover {
  background-color: #2980b9;
  transform: scale(1.05);
}

.btn.secondary {
  background-color: #7f8c8d;
  color: white;
}

.btn.secondary:hover {
  background-color: #6c7778;
  transform: scale(1.05);
}

.btn.record {
  background-color: #2ecc71;
  color: white;
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn.record.recording {
  background-color: #e74c3c;
}

.btn.record:hover {
  transform: scale(1.05);
}

.mic-icon {
  font-size: 1.2em;
  transition: transform 0.3s ease;
}

.mic-icon.active {
  transform: scale(1.2);
  color: #f1c40f;
}

.result-text {
  margin-top: 20px;
  font-size: 1.2em;
  color: #34495e;
  background: #ecf0f1;
  padding: 10px;
  border-radius: 5px;
}

.error {
  margin-top: 20px;
  font-size: 1em;
  color: #e74c3c;
  background: #fadbd8;
  padding: 10px;
  border-radius: 5px;
}

.status-section h2,
.history-section h2,
.about-section h2 {
  font-size: 1.5em;
  color: #2c3e50;
  margin-bottom: 15px;
}

.status-section p,
.about-section p {
  font-size: 1em;
  color: #34495e;
  margin: 5px 0;
}

.history-list {
  list-style: none;
  padding: 0;
  text-align: left;
}

.history-item {
  font-size: 1em;
  color: #34495e;
  padding: 8px;
  background: #ecf0f1;
  margin: 5px 0;
  border-radius: 5px;
}

.no-history {
  font-size: 1em;
  color: #7f8c8d;
}

.github-link {
  margin-top: 30px;
  font-size: 0.9em;
}

.github-link a {
  color: #3498db;
  text-decoration: none;
  padding: 5px 10px;
  border: 1px solid #3498db;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.github-link a:hover {
  background-color: #3498db;
  color: white;
  transform: scale(1.05);
}
</style>