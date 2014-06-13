function [  ] = plotLog( fileName )

    [ latitude, longitude, altitude, accuracy ] = readLog( fileName );

    subplot(2,2,1);
      plot(latitude);
      title('latitude');
      
      subplot(2,2,2);
      plot(longitude);
      title('longitude');
      
      subplot(2,2,3);
      plot(altitude);
      title('altitude');
      
      subplot(2,2,4);
      plot(accuracy);
      title('accuracy');
    
end

