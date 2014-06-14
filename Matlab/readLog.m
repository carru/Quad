function [ latitude, longitude, altitude, accuracy ] = readLog( fileName )

    fd = fopen(fileName,'r');

    data = fscanf(fd,'%f;%f;%f;%f%*s');
    
    latitude = data(1:4:550);
    longitude = data(2:4:550);
    altitude = data(3:4:550);
    accuracy = data(4:4:550);

    fclose(fd);
    
    maxLatitudeDelta = max(latitude) - min(latitude)
    maxLongitudeDelta = max(longitude) - min(longitude)
    
end
