import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';

part 'saved_screen.g.dart';

@riverpod
class SavedNumbers extends _$SavedNumbers {
  static const _key = 'saved_numbers';

  @override
  Future<List<List<int>>> build() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getStringList(_key) ?? [];
    return raw
        .map((s) => (jsonDecode(s) as List).cast<int>())
        .toList();
  }

  Future<void> add(List<int> numbers) async {
    final current = await future;
    final prefs = await SharedPreferences.getInstance();
    final updated = [...current, numbers];
    await prefs.setStringList(_key, updated.map(jsonEncode).toList());
    state = AsyncValue.data(updated);
  }

  Future<void> remove(int index) async {
    final current = await future;
    final prefs = await SharedPreferences.getInstance();
    final updated = [...current]..removeAt(index);
    await prefs.setStringList(_key, updated.map(jsonEncode).toList());
    state = AsyncValue.data(updated);
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
        error: (e, _) => Center(child: Text(e.toString())),
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
                  numbers: list[i],
                  index: i,
                  onDelete: () =>
                      ref.read(savedNumbersProvider.notifier).remove(i),
                ),
              ),
      ),
    );
  }
}

class _SavedTile extends StatelessWidget {
  const _SavedTile({
    required this.numbers,
    required this.index,
    required this.onDelete,
  });
  final List<int> numbers;
  final int index;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: Text(
          '${index + 1}',
          style: const TextStyle(color: Colors.grey, fontWeight: FontWeight.bold),
        ),
        title: Wrap(
          spacing: 6,
          children: numbers.map((n) => _Ball(n)).toList(),
        ),
        trailing: IconButton(
          icon: const Icon(Icons.delete_outline, color: Colors.red),
          onPressed: onDelete,
        ),
      ),
    );
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
