import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../core/api/api_client.dart';
import '../domain/recommend_request.dart';
import '../domain/recommended_numbers.dart';

part 'numbers_screen.g.dart';

@riverpod
class RecommendNotifier extends _$RecommendNotifier {
  @override
  AsyncValue<RecommendedNumbers?> build() => const AsyncValue.data(null);

  Future<void> recommend(RecommendRequest request) async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      final res = await ref.read(kraftApiClientProvider).recommend(request);
      return res.data!;
    });
  }
}

class NumbersScreen extends ConsumerStatefulWidget {
  const NumbersScreen({super.key});

  @override
  ConsumerState<NumbersScreen> createState() => _NumbersScreenState();
}

class _NumbersScreenState extends ConsumerState<NumbersScreen> {
  int _count = 5;

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(recommendNotifierProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('번호 추천')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('조합 수: $_count줄'),
                  Slider(
                    value: _count.toDouble(),
                    min: 1,
                    max: 10,
                    divisions: 9,
                    label: '$_count',
                    onChanged: (v) => setState(() => _count = v.round()),
                  ),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: state.isLoading
                          ? null
                          : () => ref
                              .read(recommendNotifierProvider.notifier)
                              .recommend(RecommendRequest(count: _count)),
                      icon: state.isLoading
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.auto_awesome),
                      label: const Text('번호 생성'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          state.when(
            loading: () => const SizedBox.shrink(),
            error: (e, _) => Card(
              child: ListTile(
                leading: const Icon(Icons.error_outline, color: Colors.red),
                title: Text(e.toString()),
              ),
            ),
            data: (result) => result == null
                ? const SizedBox.shrink()
                : _ResultList(result: result),
          ),
        ],
      ),
    );
  }
}

class _ResultList extends StatelessWidget {
  const _ResultList({required this.result});
  final RecommendedNumbers result;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '추천 결과 (${result.combinations.length}줄)',
          style: Theme.of(context).textTheme.titleSmall,
        ),
        const SizedBox(height: 8),
        ...result.combinations.asMap().entries.map(
              (e) => Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      Text(
                        '${e.key + 1}',
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          color: Colors.grey,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Wrap(
                          spacing: 6,
                          children: e.value.numbers.map((n) => _Ball(n)).toList(),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.bookmark_add_outlined),
                        onPressed: () {
                          // TODO: 저장 기능
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('번호함에 저장됐습니다')),
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ),
            ),
      ],
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
      width: 36,
      height: 36,
      decoration: BoxDecoration(color: _color, shape: BoxShape.circle),
      alignment: Alignment.center,
      child: Text(
        '$number',
        style: const TextStyle(
          color: Colors.white,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
