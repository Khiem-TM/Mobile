import re

text = open('d:/Direc of code/project/Mobile/mobile/app/src/main/java/com/vitalai/ui/screens/profile/ProfileScreen.kt', 'r', encoding='utf-8').read()

# Remove multi-line comments
text = re.sub(r'/\*.*?\*/', '', text, flags=re.DOTALL)
# Remove single-line comments
text = re.sub(r'//.*', '', text)
# Remove strings
text = re.sub(r'"(?:\\.|[^\\"])*"', '""', text)

stack = []
lines = text.split('\n')
for i, line in enumerate(lines):
    for c in line:
        if c == '{': stack.append(i+1)
        elif c == '}':
            if stack: stack.pop()
    if i+1 == 471: print('Stack at 471:', stack)
    if i+1 == 472: print('Stack at 472:', stack)
print('Unclosed { lines:', stack)
