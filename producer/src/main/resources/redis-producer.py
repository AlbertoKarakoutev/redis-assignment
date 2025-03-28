import random
from datetime import datetime, timedelta
import time
import uuid
import redis
import os

redis_host = os.environ["REDIS_HOST"]
redis_port = os.environ["REDIS_PORT"]
target_duration = timedelta(minutes = int(os.environ["PRODUCER_DURATION"]))
batch_size = int(os.environ["PRODUCER_BATCH_SIZE"])
indefinite_run = bool(os.environ["PRODUCER_PRODUCE_INDEFINITELY"])

def publisher():
    try:
        connection = redis.Redis(host=redis_host, port=redis_port)
        print(f"Connected to redis at {redis_host}:{redis_port}")
    except redis.ConnectionError:
        print("Error: Failed to connect to Redis server")
        exit(1)

    start_time = datetime.now()
    total_messages = 0

    try:
        while datetime.now() - start_time < target_duration or indefinite_run:
            p = connection.pipeline()
            print(f"Sending {batch_size} messages")
            for _ in range(batch_size):
                p.publish("messages:published", f'{{"message_id":"{str(uuid.uuid4())}"}}')
            p.execute()
            total_messages += batch_size
            time.sleep(random.uniform(0.1, 0.5))
            print(f"Sent {batch_size} messages")
    except Exception as e:
        print(f"Error: {e}")
    finally:
        print(f"Total messages published: {total_messages}")

if __name__ == "__main__":
    publisher()