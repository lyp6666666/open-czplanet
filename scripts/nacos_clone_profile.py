#!/usr/bin/env python3

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request


def http_get(base_url: str, path: str, params: dict[str, str]) -> bytes:
    query = urllib.parse.urlencode(params)
    url = f"{base_url}{path}?{query}"
    req = urllib.request.Request(url, method="GET")
    with urllib.request.urlopen(req, timeout=15) as resp:
        return resp.read()


def http_post(base_url: str, path: str, params: dict[str, str]) -> bytes:
    data = urllib.parse.urlencode(params).encode("utf-8")
    url = f"{base_url}{path}"
    req = urllib.request.Request(url, data=data, method="POST")
    with urllib.request.urlopen(req, timeout=15) as resp:
        return resp.read()


def load_page(base_url: str, namespace: str, page_no: int, page_size: int) -> dict:
    payload = http_get(
        base_url,
        "/nacos/v1/cs/configs",
        {
            "search": "accurate",
            "dataId": "",
            "group": "",
            "pageNo": str(page_no),
            "pageSize": str(page_size),
            "tenant": namespace,
        },
    )
    return json.loads(payload.decode("utf-8"))


def list_configs(base_url: str, namespace: str) -> list[dict]:
    first = load_page(base_url, namespace, 1, 200)
    items = list(first.get("pageItems", []))
    total = int(first.get("totalCount", len(items)))
    page_size = 200
    if total <= len(items):
        return items
    total_pages = (total + page_size - 1) // page_size
    for page_no in range(2, total_pages + 1):
        page = load_page(base_url, namespace, page_no, page_size)
        items.extend(page.get("pageItems", []))
    return items


def publish_config(base_url: str, namespace: str, group: str, data_id: str, content: str, config_type: str) -> str:
    payload = http_post(
        base_url,
        "/nacos/v1/cs/configs",
        {
            "tenant": namespace,
            "group": group,
            "dataId": data_id,
            "content": content,
            "type": config_type,
        },
    )
    return payload.decode("utf-8").strip()


def main() -> int:
    parser = argparse.ArgumentParser(description="Clone Nacos configs from one profile suffix to another.")
    parser.add_argument("--server", default="http://127.0.0.1:8848", help="Nacos server base URL")
    parser.add_argument("--source-namespace", required=True)
    parser.add_argument("--target-namespace", required=True)
    parser.add_argument("--source-suffix", default="-dev")
    parser.add_argument("--target-suffix", default="-prod")
    parser.add_argument("--group", default="DEFAULT_GROUP")
    parser.add_argument("--apply", action="store_true", help="Actually publish target configs")
    args = parser.parse_args()

    base_url = args.server.rstrip("/")
    try:
        configs = list_configs(base_url, args.source_namespace)
    except urllib.error.URLError as exc:
        print(f"[nacos_clone_profile] failed to query source namespace: {exc}", file=sys.stderr)
        return 1

    matched = []
    for item in configs:
        data_id = item.get("dataId", "")
        if not data_id.endswith(f"{args.source_suffix}.yaml"):
            continue
        group = item.get("group") or args.group
        if group != args.group:
            continue
        target_data_id = f"{data_id[: -len(args.source_suffix + '.yaml')]}{args.target_suffix}.yaml"
        matched.append(
            {
                "sourceDataId": data_id,
                "targetDataId": target_data_id,
                "group": group,
                "type": item.get("type") or "yaml",
                "content": item.get("content", ""),
            }
        )

    if not matched:
        print("[nacos_clone_profile] no matching configs found", file=sys.stderr)
        return 1

    print(f"[nacos_clone_profile] source namespace: {args.source_namespace}")
    print(f"[nacos_clone_profile] target namespace: {args.target_namespace}")
    print(f"[nacos_clone_profile] matched configs: {len(matched)}")
    for item in matched:
        print(f"- {item['sourceDataId']} -> {item['targetDataId']}")

    if not args.apply:
        print("[nacos_clone_profile] dry-run only, add --apply to publish")
        return 0

    failures = []
    for item in matched:
        try:
            result = publish_config(
                base_url,
                args.target_namespace,
                item["group"],
                item["targetDataId"],
                item["content"],
                item["type"],
            )
            print(f"[nacos_clone_profile] published {item['targetDataId']}: {result}")
            if result.lower() != "true":
                failures.append(item["targetDataId"])
        except urllib.error.URLError as exc:
            print(f"[nacos_clone_profile] publish failed for {item['targetDataId']}: {exc}", file=sys.stderr)
            failures.append(item["targetDataId"])

    if failures:
        print(f"[nacos_clone_profile] failed configs: {', '.join(failures)}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
