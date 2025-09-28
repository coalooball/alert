#!/usr/bin/env python3
"""
Kafka Producer for Alert System
Reads JSON data files and sends messages to Kafka topics
"""

import json
import argparse
import time
import sys
from pathlib import Path
from typing import List, Dict
from kafka import KafkaProducer
from kafka.errors import KafkaError


KAFKA_CONFIGS = {
    'network_attack': {
        'topic': 'network-attack-alerts',
        'file': 'network_attack_mock_data.json',
        'description': '网络攻击告警'
    },
    'malicious_sample': {
        'topic': 'malicious-sample-alerts',
        'file': 'malicious_sample_mock_data.json',
        'description': '恶意样本告警'
    },
    'host_behavior': {
        'topic': 'host-behavior-alerts',
        'file': 'host_behavior_mock_data.json',
        'description': '主机行为告警'
    }
}


class AlertKafkaProducer:
    def __init__(self, bootstrap_servers: str = 'localhost:9092'):
        """
        Initialize Kafka producer

        Args:
            bootstrap_servers: Kafka broker address
        """
        self.bootstrap_servers = bootstrap_servers
        self.producer = None

    def connect(self):
        """Connect to Kafka broker"""
        try:
            self.producer = KafkaProducer(
                bootstrap_servers=self.bootstrap_servers,
                value_serializer=lambda v: json.dumps(v).encode('utf-8'),
                key_serializer=lambda k: k.encode('utf-8') if k else None,
                acks='all',
                retries=3,
                max_in_flight_requests_per_connection=1,
                compression_type='gzip'
            )
            print(f"✅ Connected to Kafka broker: {self.bootstrap_servers}")
            return True
        except KafkaError as e:
            print(f"❌ Failed to connect to Kafka: {e}")
            return False

    def send_message(self, topic: str, message: Dict, key: str = None) -> bool:
        """
        Send a message to Kafka topic

        Args:
            topic: Kafka topic name
            message: Message data (dict)
            key: Message key (optional)

        Returns:
            True if successful, False otherwise
        """
        try:
            future = self.producer.send(topic, value=message, key=key)
            record_metadata = future.get(timeout=10)
            return True
        except Exception as e:
            print(f"❌ Failed to send message: {e}")
            return False

    def send_batch(self, topic: str, messages: List[Dict], delay: float = 0.1) -> Dict:
        """
        Send a batch of messages to Kafka topic

        Args:
            topic: Kafka topic name
            messages: List of message dictionaries
            delay: Delay between messages in seconds

        Returns:
            Statistics dict with success/failure counts
        """
        stats = {'total': len(messages), 'success': 0, 'failed': 0}

        print(f"\n📤 Sending {len(messages)} messages to topic: {topic}")

        for i, message in enumerate(messages, 1):
            key = message.get('alarm_id', f'msg-{i}')

            if self.send_message(topic, message, key):
                stats['success'] += 1
                if i % 100 == 0:
                    print(f"   Progress: {i}/{len(messages)} messages sent...")
            else:
                stats['failed'] += 1

            if delay > 0:
                time.sleep(delay)

        print(f"\n✅ Completed: {stats['success']} success, {stats['failed']} failed")
        return stats

    def close(self):
        """Close Kafka producer connection"""
        if self.producer:
            self.producer.flush()
            self.producer.close()
            print("✅ Kafka producer closed")


def load_json_file(file_path: Path) -> List[Dict]:
    """
    Load JSON data from file

    Args:
        file_path: Path to JSON file

    Returns:
        List of message dictionaries
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
            print(f"✅ Loaded {len(data)} records from {file_path.name}")
            return data
    except FileNotFoundError:
        print(f"❌ File not found: {file_path}")
        return []
    except json.JSONDecodeError as e:
        print(f"❌ Invalid JSON format: {e}")
        return []


def main():
    parser = argparse.ArgumentParser(
        description='Send alert messages to Kafka topics',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Send all network attack alerts
  %(prog)s --type network_attack

  # Send first 100 malicious sample alerts
  %(prog)s --type malicious_sample --limit 100

  # Send to custom Kafka broker
  %(prog)s --type host_behavior --broker kafka.example.com:9092

  # Send all types
  %(prog)s --all

  # Send with custom delay between messages
  %(prog)s --type network_attack --delay 0.5
        """
    )

    parser.add_argument(
        '--broker',
        default='localhost:9092',
        help='Kafka broker address (default: localhost:9092)'
    )

    parser.add_argument(
        '--type',
        choices=list(KAFKA_CONFIGS.keys()),
        help='Alert type to send'
    )

    parser.add_argument(
        '--all',
        action='store_true',
        help='Send all alert types'
    )

    parser.add_argument(
        '--limit',
        type=int,
        help='Limit number of messages to send'
    )

    parser.add_argument(
        '--delay',
        type=float,
        default=0.1,
        help='Delay between messages in seconds (default: 0.1)'
    )

    parser.add_argument(
        '--data-dir',
        type=Path,
        default=Path(__file__).parent,
        help='Directory containing JSON data files'
    )

    args = parser.parse_args()

    if not args.type and not args.all:
        parser.error("Either --type or --all must be specified")

    # Initialize Kafka producer
    producer = AlertKafkaProducer(args.broker)
    if not producer.connect():
        sys.exit(1)

    try:
        # Determine which types to send
        types_to_send = list(KAFKA_CONFIGS.keys()) if args.all else [args.type]

        total_stats = {'total': 0, 'success': 0, 'failed': 0}

        for alert_type in types_to_send:
            config = KAFKA_CONFIGS[alert_type]
            file_path = args.data_dir / config['file']

            print(f"\n{'='*60}")
            print(f"📋 Processing: {config['description']}")
            print(f"   Topic: {config['topic']}")
            print(f"   File: {file_path.name}")
            print(f"{'='*60}")

            # Load data
            messages = load_json_file(file_path)
            if not messages:
                continue

            # Apply limit if specified
            if args.limit:
                messages = messages[:args.limit]
                print(f"⚠️  Limited to first {args.limit} messages")

            # Send messages
            stats = producer.send_batch(config['topic'], messages, args.delay)

            # Update total stats
            total_stats['total'] += stats['total']
            total_stats['success'] += stats['success']
            total_stats['failed'] += stats['failed']

        # Print summary
        print(f"\n{'='*60}")
        print(f"📊 Summary")
        print(f"{'='*60}")
        print(f"Total messages: {total_stats['total']}")
        print(f"✅ Success: {total_stats['success']}")
        print(f"❌ Failed: {total_stats['failed']}")
        print(f"Success rate: {total_stats['success']/total_stats['total']*100:.2f}%")

    except KeyboardInterrupt:
        print("\n\n⚠️  Interrupted by user")
    finally:
        producer.close()


if __name__ == '__main__':
    main()