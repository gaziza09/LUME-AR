<template>
  <div class="vosk-client">
    <h1>Vosk WebSocket Client</h1>
    <button @click="requestPermission">{{ isRecording ? "Остановить запись" : "Записать голос" }}</button>
    <p v-if="result"><strong>Распознанный текст:</strong> {{ result }}</p>
    <p v-if="error">
      {{ error }}
    </p>
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

    };
  },
  methods: {
    async requestPermission() {
      try {
        // Запрашиваем доступ к микрофону
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        stream.getTracks().forEach((track) => track.stop()); // Останавливаем поток после проверки
        this.permissionDenied = false;
        this.toggleRecording(); // Начинаем запись, если разрешение получено
      } catch (error) {
        this.permissionDenied = true;
        this.error = error
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

        // Настройка WebSocket
        this.ws = new WebSocket("wss://10.8.20.49:8000/ws");
        this.ws.binaryType = "arraybuffer";

        this.ws.onmessage = (event) => {
          const data = JSON.parse(event.data);
          this.result = data.text || data.partial || "Нет распознанного текста.";
        };

        this.ws.onerror = (error) => console.error("Ошибка WebSocket:", error);
        this.ws.onclose = () => console.log("WebSocket соединение закрыто.");

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
.vosk-client {
  max-width: 500px;
  margin: 50px auto;
  text-align: center;
  font-family: Arial, sans-serif;
}

button {
  padding: 10px 20px;
  margin: 5px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

p {
  margin-top: 20px;
  font-size: 1.2em;
}

.error {
  color: red;
  font-size: 1em;
}
</style>
