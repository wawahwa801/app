"""Original AIME-style problems, each paired with a brute-force solver.

These are written for this project; none is copied from a real contest (MAA
holds the copyright on actual AIME papers). As on the AIME, every answer is an
integer from 0 to 999.

`solve` must compute the answer independently of any stored value, so that
`build_problems.py` can prove that what the app checks against is correct.
"""
from fractions import Fraction
from itertools import combinations, permutations, product
from math import comb, gcd, isqrt


def p_sum_digits_divisible():
    return sum(1 for n in range(1, 10000) if sum(map(int, str(n))) == 20 and n % 7 == 0) % 1000


def p_ordered_triples_product():
    n = 1800
    return sum(
        1
        for a in range(1, n + 1)
        if n % a == 0
        for b in range(1, n // a + 1)
        if (n // a) % b == 0
    ) % 1000


def p_power_remainder():
    return pow(7, 2026, 1000)


def p_lattice_points_circle():
    return sum(1 for x in range(-70, 71) for y in range(-70, 71) if x * x + y * y <= 65 * 65 and gcd(abs(x), abs(y)) == 1) % 1000


def p_subsets_sum_mod():
    return sum(1 for r in range(13) for s in combinations(range(1, 13), r) if sum(s) % 13 == 0) % 1000


def p_no_adjacent_binary():
    return sum(
        1
        for bits in product((0, 1), repeat=15)
        if all(not (bits[i] and bits[i + 1]) for i in range(14)) and sum(bits) == 5
    ) % 1000


def p_quadratic_integer_roots():
    values = set()
    for r in range(-2024, 2025):
        if r != 0 and 2024 % abs(r) == 0:
            s = 2024 // r
            if r <= s:
                values.add(r + s)
    return sum(abs(v) for v in values) % 1000


def p_fraction_sum():
    total = sum(Fraction(1, k * (k + 1) * (k + 2)) for k in range(1, 41))
    return (total.numerator + total.denominator) % 1000


def p_right_triangles():
    count = 0
    for a in range(1, 700):
        for b in range(a, 700):
            c2 = a * a + b * b
            c = isqrt(c2)
            if c * c == c2 and a + b + c == 720:
                count += 1
    return count % 1000


def p_triangle_integer_sides():
    return sum(1 for a in range(1, 100) for b in range(a, 100) for c in [100 - a - b] if c >= b and a + b > c) % 1000


def p_divisors_product_square():
    return sum(1 for d in range(1, 3601) if 3600 % d == 0 and isqrt(d) ** 2 == d) * 37 % 1000


def p_permutation_fixed_points():
    return sum(1 for p in permutations(range(7)) if sum(1 for i, v in enumerate(p) if i == v) == 2) % 1000


def p_tetrahedron_paths():
    # Walks of length 10 on K4 returning to the start.
    v = [1, 0, 0, 0]
    for _ in range(10):
        v = [sum(v) - v[i] for i in range(4)]
    return v[0] % 1000


def p_grid_paths_avoid():
    grid = [[0] * 9 for _ in range(9)]
    grid[0][0] = 1
    for x in range(9):
        for y in range(9):
            if (x, y) == (0, 0) or (x, y) == (4, 4):
                continue
            grid[x][y] = (grid[x - 1][y] if x else 0) + (grid[x][y - 1] if y else 0)
    return grid[8][8] % 1000


def p_sum_of_squares_mod():
    return sum(k * k for k in range(1, 2027)) % 1000


def p_vieta_power_sum():
    # x^2 - 5x + 3 = 0, s_n = 5 s_{n-1} - 3 s_{n-2}
    a, b = 2, 5
    for _ in range(9):
        a, b = b, 5 * b - 3 * a
    return b % 1000


def p_arithmetic_geometric():
    best = []
    for a in range(1, 200):
        for d in range(1, 200):
            if a * (a + 2 * d) == (a + d) ** 2 - 64 and a + 2 * d < 200:
                best.append(a + (a + d) + (a + 2 * d))
    return max(best) % 1000


def p_floor_sum():
    return sum(k * k // 17 for k in range(1, 60)) % 1000


def p_counting_multiples():
    return sum(1 for n in range(1, 10001) if (n % 6 == 0) ^ (n % 10 == 0) ^ (n % 15 == 0)) % 1000


def p_committee():
    return sum(comb(6, k) * comb(8, 7 - k) for k in range(2, 6)) % 1000


def p_palindromes_divisible():
    return sum(1 for n in range(10000, 100000) if str(n) == str(n)[::-1] and n % 11 == 0) % 1000


def p_dice_probability():
    good = sum(1 for r in product(range(1, 7), repeat=5) if sum(r) == 15)
    f = Fraction(good, 6 ** 5)
    return (f.numerator + f.denominator) % 1000


def p_coin_flips_run():
    good = 0
    for flips in product((0, 1), repeat=10):
        s = "".join(map(str, flips))
        if "111" not in s:
            good += 1
    f = Fraction(good, 2 ** 10)
    return (f.numerator + f.denominator) % 1000


def p_triangle_inradius():
    # 13-14-15 scaled by 6, computed exactly with Heron's formula.
    a, b, c = 78, 84, 90
    s = Fraction(a + b + c, 2)
    area_sq = s * (s - a) * (s - b) * (s - c)
    area = Fraction(isqrt(area_sq.numerator), isqrt(area_sq.denominator))
    assert area * area == area_sq
    r = area / s
    big_r = Fraction(a * b * c) / (4 * area)
    total = r + big_r
    return (total.numerator + total.denominator) % 1000


def p_trapezoid_area():
    # A tangential trapezoid's legs sum to its bases' sum, so leg = (b1 + b2) / 2.
    b1, b2 = 18, 50
    leg = Fraction(b1 + b2, 2)
    half_diff = Fraction(b2 - b1, 2)
    h2 = leg * leg - half_diff * half_diff
    h = Fraction(isqrt(h2.numerator), isqrt(h2.denominator))
    assert h * h == h2
    return int((b1 + b2) * h / 2) % 1000


def p_coordinate_area():
    pts = [(0, 0), (9, 2), (14, 11), (6, 17), (-3, 8)]
    twice = abs(sum(pts[i][0] * pts[(i + 1) % 5][1] - pts[(i + 1) % 5][0] * pts[i][1] for i in range(5)))
    return twice % 1000


def p_complex_power():
    # (3+4i)^6 with exact integers.
    re, im = 1, 0
    for _ in range(6):
        re, im = 3 * re - 4 * im, 4 * re + 3 * im
    return abs(re + im) % 1000


def p_log_equation():
    # log_2(x) + log_4(x) + log_16(x) = 21 means (7/4) log_2 x = 21.
    return next(k for k in range(1, 100) if Fraction(7, 4) * k == 21) ** 2


def p_functional_recursion():
    f = {1: 1}
    for n in range(2, 301):
        f[n] = f[n // 2] + (n % 2) * 3 + 1
    return sum(f[n] for n in range(1, 301)) % 1000


def p_digit_product():
    return sum(
        1
        for n in range(100, 1000)
        if (int(str(n)[0]) * int(str(n)[1]) * int(str(n)[2])) in (24, 36)
    ) % 1000


PROBLEMS = [
    ("number-theory", "Find the number of positive integers less than 10000 whose digits sum to 20 and which are divisible by 7.", p_sum_digits_divisible),
    ("number-theory", "Find the number of ordered triples of positive integers (a, b, c) such that abc = 1800. Report the remainder when this number is divided by 1000.", p_ordered_triples_product),
    ("number-theory", "Find the remainder when 7^2026 is divided by 1000.", p_power_remainder),
    ("number-theory", "Find the number of points (x, y) with integer coordinates and gcd(|x|, |y|) = 1 that satisfy x^2 + y^2 <= 65^2. Report the remainder when this number is divided by 1000. (Take gcd(0, 1) = 1 and gcd(0, 0) = 0.)", p_lattice_points_circle),
    ("number-theory", "Find the sum of k^2 for k = 1, 2, ..., 2026. Report the remainder when this sum is divided by 1000.", p_sum_of_squares_mod),
    ("number-theory", "Find the value of the sum of floor(k^2 / 17) for k = 1, 2, ..., 59. Report the remainder when this sum is divided by 1000.", p_floor_sum),
    ("number-theory", "Find the number of positive integers n <= 10000 such that an odd number of the statements \"6 divides n\", \"10 divides n\", \"15 divides n\" are true. Report the remainder when this number is divided by 1000.", p_counting_multiples),
    ("number-theory", "Find the number of five-digit palindromes that are divisible by 11.", p_palindromes_divisible),
    ("number-theory", "Find the number of three-digit positive integers whose digits have a product equal to 24 or to 36.", p_digit_product),
    ("number-theory", "Let D be the number of positive divisors of 3600 that are perfect squares. Find the remainder when 37D is divided by 1000.", p_divisors_product_square),
    ("combinatorics", "Find the number of subsets S of {1, 2, ..., 12}, including the empty set, such that the sum of the elements of S is divisible by 13. Report the remainder when this number is divided by 1000.", p_subsets_sum_mod),
    ("combinatorics", "Find the number of binary strings of length 15 that contain exactly five 1s, no two of which are adjacent.", p_no_adjacent_binary),
    ("combinatorics", "Find the number of permutations of {1, 2, ..., 7} that have exactly 2 fixed points.", p_permutation_fixed_points),
    ("combinatorics", "An ant starts at a vertex of a regular tetrahedron. Each minute it moves along an edge to one of the other three vertices. Find the number of 10-minute walks that end at the starting vertex. Report the remainder when this number is divided by 1000.", p_tetrahedron_paths),
    ("combinatorics", "Find the number of paths from (0, 0) to (8, 8), using unit steps right or up, that do not pass through (4, 4). Report the remainder when this number is divided by 1000.", p_grid_paths_avoid),
    ("combinatorics", "A committee of 7 is chosen from 6 mathematicians and 8 physicists. It must include at least 2 and at most 5 mathematicians. Find the number of possible committees. Report the remainder when this number is divided by 1000.", p_committee),
    ("combinatorics", "Five fair six-sided dice are rolled. The probability that their sum is 15 is m/n, where m and n are relatively prime positive integers. Find the remainder when m + n is divided by 1000.", p_dice_probability),
    ("combinatorics", "A fair coin is flipped 10 times. The probability that three consecutive heads never occur is m/n, where m and n are relatively prime positive integers. Find the remainder when m + n is divided by 1000.", p_coin_flips_run),
    ("algebra", "Let r and s be integers with r <= s and rs = 2024, and consider every value that r + s can take. Find the remainder when the sum of the absolute values of these distinct values is divided by 1000.", p_quadratic_integer_roots),
    ("algebra", "The sum of 1 / (k(k+1)(k+2)) for k = 1, 2, ..., 40 equals m/n, where m and n are relatively prime positive integers. Find the remainder when m + n is divided by 1000.", p_fraction_sum),
    ("algebra", "Let a and b be the roots of x^2 - 5x + 3 = 0. Find the remainder when a^10 + b^10 is divided by 1000.", p_vieta_power_sum),
    ("algebra", "Positive integers a, a + d, a + 2d, with d positive and a + 2d < 200, satisfy a(a + 2d) = (a + d)^2 - 64. Find the greatest possible sum of the three integers. Report the remainder when this sum is divided by 1000.", p_arithmetic_geometric),
    ("algebra", "Let x be a positive real number with log_2(x) + log_4(x) + log_16(x) = 21. Find (log_2(x))^2.", p_log_equation),
    ("algebra", "Define f(1) = 1, and for n >= 2 let f(n) = f(floor(n/2)) + 1 if n is even and f(n) = f(floor(n/2)) + 4 if n is odd. Find the remainder when f(1) + f(2) + ... + f(300) is divided by 1000.", p_functional_recursion),
    ("algebra", "Let (3 + 4i)^6 = A + Bi for integers A and B. Find the remainder when |A + B| is divided by 1000.", p_complex_power),
    ("geometry", "Find the number of right triangles with integer side lengths and perimeter 720, counting congruent triangles once.", p_right_triangles),
    ("geometry", "Find the number of non-congruent triangles with positive integer side lengths and perimeter 100.", p_triangle_integer_sides),
    ("geometry", "A triangle has side lengths 78, 84, and 90. The sum of its inradius and its circumradius is m/n, where m and n are relatively prime positive integers. Find the remainder when m + n is divided by 1000.", p_triangle_inradius),
    ("geometry", "An isosceles trapezoid with bases 18 and 50 has a circle tangent to all four sides. Find the remainder when the area of the trapezoid is divided by 1000.", p_trapezoid_area),
    ("geometry", "A pentagon has vertices, in order, (0, 0), (9, 2), (14, 11), (6, 17), and (-3, 8). Its area is A. Find the remainder when 2A is divided by 1000.", p_coordinate_area),
]
