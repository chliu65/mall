local order_status_key = KEYS[1]
local new_order_status = ARGV[1]
local old_order_status = ARGV[2]
local is_exists = redis.call("EXISTS", order_status_key)
if is_exists == 1 then
    local current_order_status=redis.call("GET",order_status_key)
    if(order_status_key == order_status_key) then
        redis.call("SET",order_status_key,new_order_status)
        return new_order_status
    else
        return old_order_status
    end
else
    return 'null'
end