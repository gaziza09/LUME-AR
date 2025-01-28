import websockets
from fastapi import FastAPI, WebSocket
from vosk import Model, KaldiRecognizer
import asyncio
import json
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Разрешить любые источники
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Загрузка модели Vosk
model = Model("vosk-model-small-kz-0.15")  # Путь к модели Vosk
recognizer = KaldiRecognizer(model, 16000)  # Ожидаем 16kHz аудио

# Храним текст для проксирования между сокетами
recognized_text = None

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    global recognized_text
    await websocket.accept()
    print("Соединение установлено с клиентом WebSocket.")
    try:
        while True:
            # Получаем аудиоданные от клиента
            audio_data = await websocket.receive_bytes()
            print(f"Получено {len(audio_data)} байт аудио.")
            
            # Распознавание речи
            if recognizer.AcceptWaveform(audio_data):
                result = json.loads(recognizer.Result())
                print("Распознанный текст:", result.get("text", ""))
                recognized_text = result.get("text", "")  # Сохраняем распознанный текст
                await websocket.send_json(result)
            else:
                partial = json.loads(recognizer.PartialResult())
                print("Частичный результат:", partial.get("partial", ""))
                await websocket.send_json(partial)
    except Exception as e:
        print(f"Ошибка WebSocket: {e}")
    finally:
        print("Соединение с клиентом WebSocket закрыто.")

@app.websocket("/proxy")
async def websocket_proxy(websocket: WebSocket):
    await websocket.accept()
    print("Соединение установлено с клиентом WebSocket для прокси.")
    try:
        while True:
            # Проверяем, есть ли распознанный текст
            if recognized_text:
                print(f"Отправка текста обратно клиенту: {recognized_text}")
                await websocket.send_json({"text": recognized_text})  # Отправляем текст обратно
            else:
                print("Текст не распознан.")
                await websocket.send_json({"message": "empty words"})  # Сообщаем, что нет распознанного текста

            await asyncio.sleep(1)  # Пауза между проверками (если нужно)

    except Exception as e:
        print(f"Ошибка в прокси WebSocket: {e}")
    finally:
        print("Соединение с клиентом WebSocket для прокси закрыто.")
