for i = 1, #KEYS do
    if redis.call("EXISTS",KEYS[i])==1 then
        if ARGV[i]>redis.call("GET",KEYS[i])  then
            return KEYS[i]
        end
    else
        return KEYS[i]
    end
end
for i = 1, #KEYS do
    local current_num=tonumber( redis.call("GET",KEYS[i]))
    redis.call("SET",KEYS[i],current_num-tonumber(ARGV[i]))
end
return "success"