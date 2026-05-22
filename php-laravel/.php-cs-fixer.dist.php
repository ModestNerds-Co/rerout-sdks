<?php

declare(strict_types=1);

$finder = PhpCsFixer\Finder::create()
    ->in(__DIR__ . '/src')
    ->in(__DIR__ . '/tests')
    ->in(__DIR__ . '/config')
    ->in(__DIR__ . '/routes')
    ->name('*.php');

$config = new PhpCsFixer\Config();

return $config
    ->setRiskyAllowed(true)
    ->setRules([
        '@PSR12' => true,
        '@PSR12:risky' => true,
        'declare_strict_types' => true,
        'array_syntax' => ['syntax' => 'short'],
        'no_unused_imports' => true,
        'ordered_imports' => ['sort_algorithm' => 'alpha'],
        'single_quote' => true,
        'trailing_comma_in_multiline' => ['elements' => ['arrays', 'arguments', 'parameters']],
        'no_trailing_whitespace' => true,
        'no_whitespace_in_blank_line' => true,
        'binary_operator_spaces' => ['default' => 'single_space'],
        'blank_line_before_statement' => [
            'statements' => ['return', 'throw', 'try'],
        ],
        'native_function_invocation' => false,
        'php_unit_test_class_requires_covers' => false,
    ])
    ->setFinder($finder);
