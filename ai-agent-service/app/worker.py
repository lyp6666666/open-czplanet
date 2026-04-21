from __future__ import annotations

from rq import Worker

from app.queue.redis_queue import get_queue, get_redis
from app.storage.database import init_database


def main() -> None:
    init_database()
    queue = get_queue()
    worker = Worker([queue], connection=get_redis())
    worker.work()


if __name__ == "__main__":
    main()
