globalThis.SPEEDBUMP_PROBLEMS = [
  {
    "id": 1,
    "topic": "number-theory",
    "text": "Find the number of positive integers less than 10000 whose digits sum to 20 and which are divisible by 7.",
    "answer": 90
  },
  {
    "id": 2,
    "topic": "number-theory",
    "text": "Find the number of ordered triples of positive integers (a, b, c) such that abc = 1800. Report the remainder when this number is divided by 1000.",
    "answer": 360
  },
  {
    "id": 3,
    "topic": "number-theory",
    "text": "Find the remainder when 7^2026 is divided by 1000.",
    "answer": 649
  },
  {
    "id": 4,
    "topic": "number-theory",
    "text": "Find the number of points (x, y) with integer coordinates and gcd(|x|, |y|) = 1 that satisfy x^2 + y^2 <= 65^2. Report the remainder when this number is divided by 1000. (Take gcd(0, 1) = 1 and gcd(0, 0) = 0.)",
    "answer": 72
  },
  {
    "id": 5,
    "topic": "number-theory",
    "text": "Find the sum of k^2 for k = 1, 2, ..., 2026. Report the remainder when this sum is divided by 1000.",
    "answer": 201
  },
  {
    "id": 6,
    "topic": "number-theory",
    "text": "Find the value of the sum of floor(k^2 / 17) for k = 1, 2, ..., 59. Report the remainder when this sum is divided by 1000.",
    "answer": 102
  },
  {
    "id": 7,
    "topic": "number-theory",
    "text": "Find the number of positive integers n <= 10000 such that an odd number of the statements \"6 divides n\", \"10 divides n\", \"15 divides n\" are true. Report the remainder when this number is divided by 1000.",
    "answer": 666
  },
  {
    "id": 8,
    "topic": "number-theory",
    "text": "Find the number of five-digit palindromes that are divisible by 11.",
    "answer": 82
  },
  {
    "id": 9,
    "topic": "number-theory",
    "text": "Find the number of three-digit positive integers whose digits have a product equal to 24 or to 36.",
    "answer": 42
  },
  {
    "id": 10,
    "topic": "number-theory",
    "text": "Let D be the number of positive divisors of 3600 that are perfect squares. Find the remainder when 37D is divided by 1000.",
    "answer": 444
  },
  {
    "id": 11,
    "topic": "combinatorics",
    "text": "Find the number of subsets S of {1, 2, ..., 12}, including the empty set, such that the sum of the elements of S is divisible by 13. Report the remainder when this number is divided by 1000.",
    "answer": 316
  },
  {
    "id": 12,
    "topic": "combinatorics",
    "text": "Find the number of binary strings of length 15 that contain exactly five 1s, no two of which are adjacent.",
    "answer": 462
  },
  {
    "id": 13,
    "topic": "combinatorics",
    "text": "Find the number of permutations of {1, 2, ..., 7} that have exactly 2 fixed points.",
    "answer": 924
  },
  {
    "id": 14,
    "topic": "combinatorics",
    "text": "An ant starts at a vertex of a regular tetrahedron. Each minute it moves along an edge to one of the other three vertices. Find the number of 10-minute walks that end at the starting vertex. Report the remainder when this number is divided by 1000.",
    "answer": 763
  },
  {
    "id": 15,
    "topic": "combinatorics",
    "text": "Find the number of paths from (0, 0) to (8, 8), using unit steps right or up, that do not pass through (4, 4). Report the remainder when this number is divided by 1000.",
    "answer": 970
  },
  {
    "id": 16,
    "topic": "combinatorics",
    "text": "A committee of 7 is chosen from 6 mathematicians and 8 physicists. It must include at least 2 and at most 5 mathematicians. Find the number of possible committees. Report the remainder when this number is divided by 1000.",
    "answer": 248
  },
  {
    "id": 17,
    "topic": "combinatorics",
    "text": "Five fair six-sided dice are rolled. The probability that their sum is 15 is m/n, where m and n are relatively prime positive integers. Find the remainder when m + n is divided by 1000.",
    "answer": 809
  },
  {
    "id": 18,
    "topic": "combinatorics",
    "text": "A fair coin is flipped 10 times. The probability that three consecutive heads never occur is m/n, where m and n are relatively prime positive integers. Find the remainder when m + n is divided by 1000.",
    "answer": 191
  },
  {
    "id": 19,
    "topic": "algebra",
    "text": "Let r and s be integers with r <= s and rs = 2024, and consider every value that r + s can take. Find the remainder when the sum of the absolute values of these distinct values is divided by 1000.",
    "answer": 640
  },
  {
    "id": 20,
    "topic": "algebra",
    "text": "The sum of 1 / (k(k+1)(k+2)) for k = 1, 2, ..., 40 equals m/n, where m and n are relatively prime positive integers. Find the remainder when m + n is divided by 1000.",
    "answer": 76
  },
  {
    "id": 21,
    "topic": "algebra",
    "text": "Let a and b be the roots of x^2 - 5x + 3 = 0. Find the remainder when a^10 + b^10 is divided by 1000.",
    "answer": 139
  },
  {
    "id": 22,
    "topic": "algebra",
    "text": "Positive integers a, a + d, a + 2d, with d positive and a + 2d < 200, satisfy a(a + 2d) = (a + d)^2 - 64. Find the greatest possible sum of the three integers. Report the remainder when this sum is divided by 1000.",
    "answer": 573
  },
  {
    "id": 23,
    "topic": "algebra",
    "text": "Let x be a positive real number with log_2(x) + log_4(x) + log_16(x) = 21. Find (log_2(x))^2.",
    "answer": 144
  },
  {
    "id": 24,
    "topic": "algebra",
    "text": "Define f(1) = 1, and for n >= 2 let f(n) = f(floor(n/2)) + 1 if n is even and f(n) = f(floor(n/2)) + 4 if n is odd. Find the remainder when f(1) + f(2) + ... + f(300) is divided by 1000.",
    "answer": 850
  },
  {
    "id": 25,
    "topic": "algebra",
    "text": "Let (3 + 4i)^6 = A + Bi for integers A and B. Find the remainder when |A + B| is divided by 1000.",
    "answer": 457
  },
  {
    "id": 26,
    "topic": "geometry",
    "text": "Find the number of right triangles with integer side lengths and perimeter 720, counting congruent triangles once.",
    "answer": 6
  },
  {
    "id": 27,
    "topic": "geometry",
    "text": "Find the number of non-congruent triangles with positive integer side lengths and perimeter 100.",
    "answer": 208
  },
  {
    "id": 28,
    "topic": "geometry",
    "text": "A triangle has side lengths 78, 84, and 90. The sum of its inradius and its circumradius is m/n, where m and n are relatively prime positive integers. Find the remainder when m + n is divided by 1000.",
    "answer": 295
  },
  {
    "id": 29,
    "topic": "geometry",
    "text": "An isosceles trapezoid with bases 18 and 50 has a circle tangent to all four sides. Find the remainder when the area of the trapezoid is divided by 1000.",
    "answer": 20
  },
  {
    "id": 30,
    "topic": "geometry",
    "text": "A pentagon has vertices, in order, (0, 0), (9, 2), (14, 11), (6, 17), and (-3, 8). Its area is A. Find the remainder when 2A is divided by 1000.",
    "answer": 342
  }
];
