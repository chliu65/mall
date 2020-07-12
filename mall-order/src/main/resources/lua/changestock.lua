local stock = KEYS[1]
local stock_change = tonumber(ARGV[1])
local is_exists = redis.call("EXISTS", stock)
if is_exists == 1 then
    local stockAftChange = tonumber(redis.call("GET", stock)) - stock_change
    if(stockAftChange<0) then
        return -1
    else
        redis.call("SET", stock,tostring(stockAftChange))
        return stockAftChange
    end
else
    return -2
end