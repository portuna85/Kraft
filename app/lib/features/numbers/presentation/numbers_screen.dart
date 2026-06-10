import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../core/api/api_client.dart';
import '../../../core/error/app_exception.dart';
import '../domain/recommend_request.dart';
import '../domain/recommended_numbers.dart';
import '../../saved/presentation/saved_screen.dart';

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
  bool _showAdvanced = false;
  int? _oddCount;
  int? _sumMin;
  int? _sumMax;

  final _sumMinController = TextEditingController();
  final _sumMaxController = TextEditingController();

  @override
  void dispose() {
    _sumMinController.dispose();
    _sumMaxController.dispose();
    super.dispose();
  }

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
                  InkWell(
                    onTap: () =>
                        setState(() => _showAdvanced = !_showAdvanced),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Row(
                        children: [
                          Text(
                            '고급 옵션',
                            style: TextStyle(
                              color: Theme.of(context).colorScheme.primary,
                              fontSize: 13,
                            ),
                          ),
                          Icon(
                            _showAdvanced
                                ? Icons.expand_less
                                : Icons.expand_more,
                            size: 18,
                            color: Theme.of(context).colorScheme.primary,
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (_showAdvanced) ...[
                    const SizedBox(height: 8),
                    _OddCountSelector(
                      value: _oddCount,
                      onChanged: (v) => setState(() => _oddCount = v),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _sumMinController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: '합계 최소',
                              isDense: true,
                              border: OutlineInputBorder(),
                            ),
                            onChanged: (v) => setState(
                                () => _sumMin = int.tryParse(v)),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: TextField(
                            controller: _sumMaxController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: '합계 최대',
                              isDense: true,
                              border: OutlineInputBorder(),
                            ),
                            onChanged: (v) => setState(
                                () => _sumMax = int.tryParse(v)),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                  ],
                  const SizedBox(height: 4),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: state.isLoading
                          ? null
                          : () => ref
                              .read(recommendNotifierProvider.notifier)
                              .recommend(RecommendRequest(
                                count: _count,
                                oddCount: _oddCount,
                                sumMin: _sumMin,
                                sumMax: _sumMax,
                              )),
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
                title: Text(errorMessage(e)),
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

class _ResultList extends ConsumerWidget {
  const _ResultList({required this.result});
  final RecommendedNumbers result;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
                        onPressed: () async {
                          await ref
                              .read(savedNumbersProvider.notifier)
                              .add(e.value.numbers);
                          if (context.mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('번호함에 저장됐습니다')),
                            );
                          }
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

class _OddCountSelector extends StatelessWidget {
  const _OddCountSelector({required this.value, required this.onChanged});
  final int? value;
  final ValueChanged<int?> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('홀수 개수', style: TextStyle(fontSize: 13)),
        const SizedBox(height: 4),
        Wrap(
          spacing: 6,
          children: [
            ChoiceChip(
              label: const Text('제한없음'),
              selected: value == null,
              onSelected: (_) => onChanged(null),
            ),
            ...List.generate(
              7,
              (i) => ChoiceChip(
                label: Text('$i개'),
                selected: value == i,
                onSelected: (_) => onChanged(i),
              ),
            ),
          ],
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
