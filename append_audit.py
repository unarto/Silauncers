import sys

def append_to_audit(content):
    with open("audit rsp+bug.md", "a", encoding="utf-8") as f:
        f.write("\n" + content + "\n")

if __name__ == "__main__":
    append_to_audit(sys.argv[1])
