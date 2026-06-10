import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../core/api/api_client.dart';
import '../../../core/error/app_exception.dart';

part 'saved_screen.g.dart';

// FCM 토큰 키 (push_notification_service.dart 와 동일)
const _tokenKey = 'fcm_token_v1';
const _storage = FlutterSecureStorage();

class SavedItem {
  const SavedItem({
    required this.id,
    required this.numbers,
    this.label,
    required this.savedAt,
  });

  final int id;
  final List<int> numbers;
  final String? label;
  final DateTime savedAt;

  factory SavedItem.fromJson(Map<String, dynamic> j) => SavedItem(
        id: j['id'] as int,
        numbers: (j['numbers'] as List).cast<int>(),
        label: j['label'] as String?,
        savedAt: DateTime.parse(j['savedAt'] as String),
      );
}

@riverpod
class SavedNumbers extends _$SavedNumbers {
  @override
  Future<List<SavedItem>> build() async {
    final token = await _storage.read(key: _tokenKey);
    if (token == null) return [];
    final api = ref.watch(kraftApiClientProvider);
    final res = await api.getSavedNumbers(token);
    return res.data?.map(SavedItem.fromJson).toList() ?? [];
  }

  Future<void> add(List<int> numbers, {String? label}) async {
    final token = await _storage.read(key: _tokenKey);
    if (token == null) throw Exception('FCM 토큰이 없습니다. 푸시 알림 설정을 확인하세요.');
    final api = ref.read(kraftApiClientProvider);
    final res = await api.saveSavedNumbers(token, numbers, label);
    if (res.data != null) {
      final newItem = SavedItem.fromJson(res.data!);
      final current = await future;
      state = AsyncValue.data([newItem, ...current]);
    }
  }

  Future<void> remove(int id) async {
    final token = await _storage.read(key: _tokenKey);
    if (token == null) return;
    final api = ref.read(kraftApiClientProvider);
    await api.deleteSavedNumbers(id, token);
    final current = await future;
    state = AsyncValue.data(current.where((e) => e.id != id).toList());
  }
}

class SavedScreen extends ConsumerWidget {
  const SavedScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(savedNumbersProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('내 번호함')),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text(errorMessage(e))),
        data: (list) => list.isEmpty
            ? const Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.bookmark_outline, size: 48, color: Colors.grey),
                    SizedBox(height: 12),
                    Text('저장된 번호가 없습니다'),
                    SizedBox(height: 4),
                    Text(
                      '번호 추천에서 북마크 버튼을 눌러 저장하세요',
                      style: TextStyle(color: Colors.grey, fontSize: 12),
                    ),
                  ],
                ),
              )
            : ListView.builder(
                padding: const EdgeInsets.all(8),
                itemCount: list.length,
                itemBuilder: (_, i) => _SavedTile(
                  item: list[i],
                  onDelete: () =>
                      ref.read(savedNumbersProvider.notifier).remove(list[i].id),
                ),
              ),
      ),
    );
  }
}

class _SavedTile extends StatelessWidget {
  const _SavedTile({required this.item, required this.onDelete});
  final SavedItem item;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: item.label != null
            ? Tooltip(
                message: item.label!,
                child: const Icon(Icons.label_outline, color: Colors.blueGrey),
              )
            : const Icon(Icons.bookmark_outline, color: Colors.grey),
        title: Wrap(
          spacing: 6,
          children: item.numbers.map((n) => _Ball(n)).toList(),
        ),
        subtitle: Text(
          _formatDate(item.savedAt),
          style: const TextStyle(fontSize: 11, color: Colors.grey),
        ),
        trailing: IconButton(
          icon: const Icon(Icons.delete_outline, color: Colors.red),
          onPressed: onDelete,
        ),
      ),
    );
  }

  String _formatDate(DateTime dt) {
    return '${dt.year}.${dt.month.toString().padLeft(2, '0')}.${dt.day.toString().padLeft(2, '0')}';
  }
}

class _Ball extends StatelessWidget {
  const _Ball(this.number);
  final int number;

  Color get _color {
    if (number <= 10) return const Color(0xFFF9A825);
    if (number <= 20) return const Color(0xFF42A5F5);
    if (number <= 30) return const Color(0xFFEF5350);
    if (number <= 40) return const Color(0xFF757575);
    return const Color(0xFF66BB6A);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(color: _color, shape: BoxShape.circle),
      alignment: Alignment.center,
      child: Text(
        '$number',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 12,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
