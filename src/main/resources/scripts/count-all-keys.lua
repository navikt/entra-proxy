local counts = {}
for i, pattern in ipairs(ARGV) do
    local cursor = "0"
    local count = 0
    repeat
        local result = redis.call("SCAN", cursor, "MATCH", pattern, "COUNT", 50000)
        cursor = result[1]
        count = count + #result[2]
    until cursor == "0"
    counts[i] = count
end
return counts

